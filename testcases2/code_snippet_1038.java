private AuthenticationMechanismOutcome handleDigestHeader(HttpServerExchange exchange, final SecurityContext securityContext) {
        DigestContext context = exchange.getAttachment(DigestContext.ATTACHMENT_KEY);
        Map<DigestAuthorizationToken, String> parsedHeader = context.getParsedHeader();
        // Step 1 - Verify the set of tokens received to ensure valid values.
        Set<DigestAuthorizationToken> mandatoryTokens = EnumSet.copyOf(MANDATORY_REQUEST_TOKENS);
        if (!supportedAlgorithms.contains(DigestAlgorithm.MD5)) {
            // If we don't support MD5 then the client must choose an algorithm as we can not fall back to MD5.
            mandatoryTokens.add(DigestAuthorizationToken.ALGORITHM);
        }
        if (!supportedQops.isEmpty() && !supportedQops.contains(DigestQop.AUTH)) {
            // If we do not support auth then we are mandating auth-int so force the client to send a QOP
            mandatoryTokens.add(DigestAuthorizationToken.MESSAGE_QOP);
        }

        DigestQop qop = null;
        // This check is early as is increases the list of mandatory tokens.
        if (parsedHeader.containsKey(DigestAuthorizationToken.MESSAGE_QOP)) {
            qop = DigestQop.forName(parsedHeader.get(DigestAuthorizationToken.MESSAGE_QOP));
            if (qop == null || !supportedQops.contains(qop)) {
                // We are also ensuring the client is not trying to force a qop that has been disabled.
                REQUEST_LOGGER.invalidTokenReceived(DigestAuthorizationToken.MESSAGE_QOP.getName(),
                        parsedHeader.get(DigestAuthorizationToken.MESSAGE_QOP));
                // TODO - This actually needs to result in a HTTP 400 Bad Request response and not a new challenge.
                return AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
            }
            context.setQop(qop);
            mandatoryTokens.add(DigestAuthorizationToken.CNONCE);
            mandatoryTokens.add(DigestAuthorizationToken.NONCE_COUNT);
        }

        // Check all mandatory tokens are present.
        mandatoryTokens.removeAll(parsedHeader.keySet());
        if (mandatoryTokens.size() > 0) {
            for (DigestAuthorizationToken currentToken : mandatoryTokens) {
                // TODO - Need a better check and possible concatenate the list of tokens - however
                // even having one missing token is not something we should routinely expect.
                REQUEST_LOGGER.missingAuthorizationToken(currentToken.getName());
            }
            // TODO - This actually needs to result in a HTTP 400 Bad Request response and not a new challenge.
            return AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
        }

        // Perform some validation of the remaining tokens.
        if (!realmName.equals(parsedHeader.get(DigestAuthorizationToken.REALM))) {
            REQUEST_LOGGER.invalidTokenReceived(DigestAuthorizationToken.REALM.getName(),
                    parsedHeader.get(DigestAuthorizationToken.REALM));
            // TODO - This actually needs to result in a HTTP 400 Bad Request response and not a new challenge.
            return AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
        }

        if(parsedHeader.containsKey(DigestAuthorizationToken.DIGEST_URI)) {
            String uri = parsedHeader.get(DigestAuthorizationToken.DIGEST_URI);
            String requestURI = exchange.getRequestURI();
            if(!exchange.getQueryString().isEmpty()) {
                requestURI = requestURI + "?" + exchange.getQueryString();
            }
            if(!uri.equals(requestURI)) {
                //just end the auth process
                exchange.setStatusCode(StatusCodes.BAD_REQUEST);
                exchange.endExchange();
                return AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
            }
        } else {
            return AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
        }

        if (parsedHeader.containsKey(DigestAuthorizationToken.OPAQUE)) {
            if (!OPAQUE_VALUE.equals(parsedHeader.get(DigestAuthorizationToken.OPAQUE))) {
                REQUEST_LOGGER.invalidTokenReceived(DigestAuthorizationToken.OPAQUE.getName(),
                        parsedHeader.get(DigestAuthorizationToken.OPAQUE));
                return AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
            }
        }

        DigestAlgorithm algorithm;
        if (parsedHeader.containsKey(DigestAuthorizationToken.ALGORITHM)) {
            algorithm = DigestAlgorithm.forName(parsedHeader.get(DigestAuthorizationToken.ALGORITHM));
            if (algorithm == null || !supportedAlgorithms.contains(algorithm)) {
                // We are also ensuring the client is not trying to force an algorithm that has been disabled.
                REQUEST_LOGGER.invalidTokenReceived(DigestAuthorizationToken.ALGORITHM.getName(),
                        parsedHeader.get(DigestAuthorizationToken.ALGORITHM));
                // TODO - This actually needs to result in a HTTP 400 Bad Request response and not a new challenge.
                return AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
            }
        } else {
            // We know this is safe as the algorithm token was made mandatory
            // if MD5 is not supported.
            algorithm = DigestAlgorithm.MD5;
        }

        try {
            context.setAlgorithm(algorithm);
        } catch (NoSuchAlgorithmException e) {
            /*
             * This should not be possible in a properly configured installation.
             */
            REQUEST_LOGGER.exceptionProcessingRequest(e);
            return AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
        }

        final String userName = parsedHeader.get(DigestAuthorizationToken.USERNAME);
        final IdentityManager identityManager = getIdentityManager(securityContext);
        final Account account;

        if (algorithm.isSession()) {
            /* This can follow one of the following: -
             *   1 - New session so use DigestCredentialImpl with the IdentityManager to
             *       create a new session key.
             *   2 - Obtain the existing session key from the session store and validate it, just use
             *       IdentityManager to validate account is still active and the current role assignment.
             */
            throw new IllegalStateException("Not yet implemented.");
        } else {
            final DigestCredential credential = new DigestCredentialImpl(context);
            account = identityManager.verify(userName, credential);
        }

        if (account == null) {
            // Authentication has failed, this could either be caused by the user not-existing or it
            // could be caused due to an invalid hash.
            securityContext.authenticationFailed(MESSAGES.authenticationFailed(userName), mechanismName);
            return AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
        }

        // Step 3 - Verify that the nonce was eligible to be used.
        if (!validateNonceUse(context, parsedHeader, exchange)) {
            // TODO - This is the right place to make use of the decision but the check needs to be much much sooner
            // otherwise a failure server
            // side could leave a packet that could be 're-played' after the failed auth.
            // The username and password verification passed but for some reason we do not like the nonce.
            context.markStale();
            // We do not mark as a failure on the security context as this is not quite a failure, a client with a cached nonce
            // can easily hit this point.
            return AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
        }

        // We have authenticated the remote user.

        sendAuthenticationInfoHeader(exchange);
        securityContext.authenticationComplete(account, mechanismName, false);
        return AuthenticationMechanismOutcome.AUTHENTICATED;

        // Step 4 - Set up any QOP related requirements.

        // TODO - Do QOP
    }