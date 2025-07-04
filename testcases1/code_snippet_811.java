public TokenValidatorResponse validateToken(TokenValidatorParameters tokenParameters) {
        LOG.fine("Validating SAML Token");
        STSPropertiesMBean stsProperties = tokenParameters.getStsProperties();
        Crypto sigCrypto = stsProperties.getSignatureCrypto();
        CallbackHandler callbackHandler = stsProperties.getCallbackHandler();
        
        TokenValidatorResponse response = new TokenValidatorResponse();
        ReceivedToken validateTarget = tokenParameters.getToken();
        validateTarget.setState(STATE.INVALID);
        response.setToken(validateTarget);
        
        if (!validateTarget.isDOMElement()) {
            return response;
        }
        
        try {
            Element validateTargetElement = (Element)validateTarget.getToken();
            SamlAssertionWrapper assertion = new SamlAssertionWrapper(validateTargetElement);
            
            SAMLTokenPrincipal samlPrincipal = new SAMLTokenPrincipalImpl(assertion);
            response.setPrincipal(samlPrincipal);
            
            SecurityToken secToken = null;
            byte[] signatureValue = assertion.getSignatureValue();
            if (tokenParameters.getTokenStore() != null && signatureValue != null
                && signatureValue.length > 0) {
                int hash = Arrays.hashCode(signatureValue);
                secToken = tokenParameters.getTokenStore().getToken(Integer.toString(hash));
                if (secToken != null && secToken.getTokenHash() != hash) {
                    secToken = null;
                }
            }
            if (secToken != null && secToken.isExpired()) {
                LOG.fine("Token: " + secToken.getId() + " is in the cache but expired - revalidating");
                secToken = null;
            }
            
            if (secToken == null) {
                if (!assertion.isSigned()) {
                    LOG.log(Level.WARNING, "The received assertion is not signed, and therefore not trusted");
                    return response;
                }
                
                RequestData requestData = new RequestData();
                requestData.setSigVerCrypto(sigCrypto);
                WSSConfig wssConfig = WSSConfig.getNewInstance();
                requestData.setWssConfig(wssConfig);
                requestData.setCallbackHandler(callbackHandler);
                requestData.setMsgContext(tokenParameters.getWebServiceContext().getMessageContext());

                WSDocInfo docInfo = new WSDocInfo(validateTargetElement.getOwnerDocument());
                
                // Verify the signature
                Signature sig = assertion.getSignature();
                KeyInfo keyInfo = sig.getKeyInfo();
                SAMLKeyInfo samlKeyInfo = 
                    SAMLUtil.getCredentialFromKeyInfo(
                        keyInfo.getDOM(), new WSSSAMLKeyInfoProcessor(requestData, docInfo), sigCrypto
                    );
                assertion.verifySignature(samlKeyInfo);
                
                // Validate the assertion against schemas/profiles
                validateAssertion(assertion);

                // Now verify trust on the signature
                Credential trustCredential = new Credential();
                trustCredential.setPublicKey(samlKeyInfo.getPublicKey());
                trustCredential.setCertificates(samlKeyInfo.getCerts());
    
                trustCredential = validator.validate(trustCredential, requestData);

                // Finally check that subject DN of the signing certificate matches a known constraint
                X509Certificate cert = null;
                if (trustCredential.getCertificates() != null) {
                    cert = trustCredential.getCertificates()[0];
                }
                
                if (!certConstraints.matches(cert)) {
                    return response;
                }
                
            }
            
            // Parse roles from the validated token
            if (samlRoleParser != null) {
                Set<Principal> roles = 
                    samlRoleParser.parseRolesFromAssertion(samlPrincipal, null, assertion);
                response.setRoles(roles);
            }
           
            // Get the realm of the SAML token
            String tokenRealm = null;
            if (samlRealmCodec != null) {
                tokenRealm = samlRealmCodec.getRealmFromToken(assertion);
                // verify the realm against the cached token
                if (secToken != null) {
                    Properties props = secToken.getProperties();
                    if (props != null) {
                        String cachedRealm = props.getProperty(STSConstants.TOKEN_REALM);
                        if (cachedRealm != null && !tokenRealm.equals(cachedRealm)) {
                            return response;
                        }
                    }
                }
            }
            response.setTokenRealm(tokenRealm);
            
            if (!validateConditions(assertion, validateTarget)) {
                return response;
            }
            
            // Store the successfully validated token in the cache
            if (secToken == null) {
                storeTokenInCache(
                    tokenParameters.getTokenStore(), assertion, tokenParameters.getPrincipal(), tokenRealm
                );
            }
            
            // Add the SamlAssertionWrapper to the properties, as the claims are required to be transformed
            Map<String, Object> addProps = new HashMap<String, Object>();
            addProps.put(SamlAssertionWrapper.class.getName(), assertion);
            response.setAdditionalProperties(addProps);
            
            validateTarget.setState(STATE.VALID);
        } catch (WSSecurityException ex) {
            LOG.log(Level.WARNING, "", ex);
        }

        return response;
    }