public SSOValidatorResponse validateSamlResponse(
        org.opensaml.saml.saml2.core.Response samlResponse,
        boolean postBinding
    ) throws WSSecurityException {
        // Check the Issuer
        validateIssuer(samlResponse.getIssuer());

        // The Response must contain at least one Assertion.
        if (samlResponse.getAssertions() == null || samlResponse.getAssertions().isEmpty()) {
            LOG.fine("The Response must contain at least one Assertion");
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "invalidSAMLsecurity");
        }
        
        // The Response must contain a Destination that matches the assertionConsumerURL if it is
        // signed
        String destination = samlResponse.getDestination();
        if (samlResponse.isSigned()
            && (destination == null || !destination.equals(assertionConsumerURL))) {
            LOG.fine("The Response must contain a destination that matches the assertion consumer URL");
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "invalidSAMLsecurity");
        }
        
        // Validate Assertions
        boolean foundValidSubject = false;
        Date sessionNotOnOrAfter = null;
        for (org.opensaml.saml.saml2.core.Assertion assertion : samlResponse.getAssertions()) {
            // Check the Issuer
            if (assertion.getIssuer() == null) {
                LOG.fine("Assertion Issuer must not be null");
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "invalidSAMLsecurity");
            }
            validateIssuer(assertion.getIssuer());
            
            if (enforceAssertionsSigned && postBinding && assertion.getSignature() == null) {
                LOG.fine("If the HTTP Post binding is used to deliver the Response, "
                         + "the enclosed assertions must be signed");
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "invalidSAMLsecurity");
            }
            
            // Check for AuthnStatements and validate the Subject accordingly
            if (assertion.getAuthnStatements() != null
                && !assertion.getAuthnStatements().isEmpty()) {
                org.opensaml.saml.saml2.core.Subject subject = assertion.getSubject();
                if (validateAuthenticationSubject(subject, assertion.getID(), postBinding)) {
                    validateAudienceRestrictionCondition(assertion.getConditions());
                    foundValidSubject = true;
                    // Store Session NotOnOrAfter
                    for (AuthnStatement authnStatment : assertion.getAuthnStatements()) {
                        if (authnStatment.getSessionNotOnOrAfter() != null) {
                            sessionNotOnOrAfter = authnStatment.getSessionNotOnOrAfter().toDate();
                        }
                    }
                }
            }
            
        }
        
        if (!foundValidSubject) {
            LOG.fine("The Response did not contain any Authentication Statement that matched "
                     + "the Subject Confirmation criteria");
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "invalidSAMLsecurity");
        }
        
        SSOValidatorResponse validatorResponse = new SSOValidatorResponse();
        validatorResponse.setResponseId(samlResponse.getID());
        validatorResponse.setSessionNotOnOrAfter(sessionNotOnOrAfter);
        if (samlResponse.getIssueInstant() != null) {
            validatorResponse.setCreated(samlResponse.getIssueInstant().toDate());
        }
        
        // the assumption for now is that SAMLResponse will contain only a single assertion
        Element assertionElement = samlResponse.getAssertions().get(0).getDOM();
        Element clonedAssertionElement = (Element)assertionElement.cloneNode(true);
        validatorResponse.setAssertionElement(clonedAssertionElement);
        validatorResponse.setAssertion(DOM2Writer.nodeToString(clonedAssertionElement));
        
        return validatorResponse;
    }