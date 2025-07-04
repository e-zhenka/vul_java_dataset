private void verifySavedState(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null) {
            logger.warn("The received state does not match the state saved in the context");
            throw new BadCredentialsException("The received state does not match the state saved in the context");
        }

        String savedContext = (String)session.getAttribute(FederationAuthenticationEntryPoint.SAVED_CONTEXT);
        String state = getState(request);
        if (savedContext == null || !savedContext.equals(state)) {
            logger.warn("The received state does not match the state saved in the context");
            throw new BadCredentialsException("The received state does not match the state saved in the context");
        }
        session.removeAttribute(FederationAuthenticationEntryPoint.SAVED_CONTEXT);
    }