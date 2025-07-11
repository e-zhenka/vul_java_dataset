protected Response handleLoginResponse(String samlResponse, SAMLDocumentHolder holder, ResponseType responseType, String relayState, String clientId) {

            try {
                KeyManager.ActiveRsaKey keys = session.keys().getActiveRsaKey(realm);
                if (! isSuccessfulSamlResponse(responseType)) {
                    String statusMessage = responseType.getStatus() == null ? Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR : responseType.getStatus().getStatusMessage();
                    return callback.error(relayState, statusMessage);
                }
                if (responseType.getAssertions() == null || responseType.getAssertions().isEmpty()) {
                    return callback.error(relayState, Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
                }

                boolean assertionIsEncrypted = AssertionUtil.isAssertionEncrypted(responseType);

                if (config.isWantAssertionsEncrypted() && !assertionIsEncrypted) {
                    logger.error("The assertion is not encrypted, which is required.");
                    event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                    event.error(Errors.INVALID_SAML_RESPONSE);
                    return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUESTER);
                }

                Element assertionElement;

                if (assertionIsEncrypted) {
                    // This methods writes the parsed and decrypted assertion back on the responseType parameter:
                    assertionElement = AssertionUtil.decryptAssertion(holder, responseType, keys.getPrivateKey());
                } else {
                    /* We verify the assertion using original document to handle cases where the IdP
                    includes whitespace and/or newlines inside tags. */
                    assertionElement = DocumentUtil.getElement(holder.getSamlDocument(), new QName(JBossSAMLConstants.ASSERTION.get()));
                }

                boolean signed = AssertionUtil.isSignedElement(assertionElement);
                if ((config.isWantAssertionsSigned() && !signed)
                        || (signed && config.isValidateSignature() && !AssertionUtil.isSignatureValid(assertionElement, getIDPKeyLocator()))) {
                    logger.error("validation failed");
                    event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                    event.error(Errors.INVALID_SIGNATURE);
                    return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUESTER);
                }

                AssertionType assertion = responseType.getAssertions().get(0).getAssertion();

                SubjectType subject = assertion.getSubject();
                SubjectType.STSubType subType = subject.getSubType();
                NameIDType subjectNameID = (NameIDType) subType.getBaseID();
                //Map<String, String> notes = new HashMap<>();
                BrokeredIdentityContext identity = new BrokeredIdentityContext(subjectNameID.getValue());
                identity.getContextData().put(SAML_LOGIN_RESPONSE, responseType);
                identity.getContextData().put(SAML_ASSERTION, assertion);
                if (clientId != null && ! clientId.trim().isEmpty()) {
                    identity.getContextData().put(SAML_IDP_INITIATED_CLIENT_ID, clientId);
                }

                identity.setUsername(subjectNameID.getValue());

                //SAML Spec 2.2.2 Format is optional
                if (subjectNameID.getFormat() != null && subjectNameID.getFormat().toString().equals(JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.get())) {
                    identity.setEmail(subjectNameID.getValue());
                }

                if (config.isStoreToken()) {
                    identity.setToken(samlResponse);
                }

                ConditionsValidator.Builder cvb = new ConditionsValidator.Builder(assertion.getID(), assertion.getConditions(), destinationValidator);
                try {
                    String issuerURL = getEntityId(session.getContext().getUri(), realm);
                    cvb.addAllowedAudience(URI.create(issuerURL));
                    // getDestination has been validated to match request URL already so it matches SAML endpoint
                    cvb.addAllowedAudience(URI.create(responseType.getDestination()));
                } catch (IllegalArgumentException ex) {
                    // warning has been already emitted in DeploymentBuilder
                }
                if (! cvb.build().isValid()) {
                    logger.error("Assertion expired.");
                    event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                    event.error(Errors.INVALID_SAML_RESPONSE);
                    return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.EXPIRED_CODE);
                }

                AuthnStatementType authn = null;
                for (Object statement : assertion.getStatements()) {
                    if (statement instanceof AuthnStatementType) {
                        authn = (AuthnStatementType)statement;
                        identity.getContextData().put(SAML_AUTHN_STATEMENT, authn);
                        break;
                    }
                }
                if (assertion.getAttributeStatements() != null ) {
                    for (AttributeStatementType attrStatement : assertion.getAttributeStatements()) {
                        for (AttributeStatementType.ASTChoiceType choice : attrStatement.getAttributes()) {
                            AttributeType attribute = choice.getAttribute();
                            if (X500SAMLProfileConstants.EMAIL.getFriendlyName().equals(attribute.getFriendlyName())
                                    || X500SAMLProfileConstants.EMAIL.get().equals(attribute.getName())) {
                                if (!attribute.getAttributeValue().isEmpty()) identity.setEmail(attribute.getAttributeValue().get(0).toString());
                            }
                        }

                    }

                }
                String brokerUserId = config.getAlias() + "." + subjectNameID.getValue();
                identity.setBrokerUserId(brokerUserId);
                identity.setIdpConfig(config);
                identity.setIdp(provider);
                if (authn != null && authn.getSessionIndex() != null) {
                    identity.setBrokerSessionId(identity.getBrokerUserId() + "." + authn.getSessionIndex());
                 }
                identity.setCode(relayState);


                return callback.authenticated(identity);
            } catch (WebApplicationException e) {
                return e.getResponse();
            } catch (Exception e) {
                throw new IdentityBrokerException("Could not process response from SAML identity provider.", e);
            }
        }