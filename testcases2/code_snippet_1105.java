@Override
    public AcceptedInvitation acceptInvitation(String code, String password) {
        ExpiringCode data = expiringCodeStore.retrieveCode(code);

        Map<String,String> userData = JsonUtils.readValue(data.getData(), new TypeReference<Map<String, String>>() {});
        String userId = userData.get(USER_ID);
        String clientId = userData.get(CLIENT_ID);
        String redirectUri = userData.get(REDIRECT_URI);

        ScimUser user = scimUserProvisioning.retrieve(userId);

        user = scimUserProvisioning.verifyUser(userId, user.getVersion());


        if (OriginKeys.UAA.equals(user.getOrigin()) && StringUtils.hasText(password)) {
            PasswordChangeRequest request = new PasswordChangeRequest();
            request.setPassword(password);
            scimUserProvisioning.changePassword(userId, null, password);
        }

        String redirectLocation = "/home";
        try {
            ClientDetails clientDetails = clientDetailsService.loadClientByClientId(clientId);
            Set<String> redirectUris = clientDetails.getRegisteredRedirectUri();
            redirectLocation = UaaUrlUtils.findMatchingRedirectUri(redirectUris, redirectUri, redirectLocation);
        } catch (NoSuchClientException x) {
            logger.debug("Unable to find client_id for invitation:"+clientId);
        } catch (Exception x) {
            logger.error("Unable to resolve redirect for clientID:"+clientId, x);
        }
        return new AcceptedInvitation(redirectLocation, user);
    }