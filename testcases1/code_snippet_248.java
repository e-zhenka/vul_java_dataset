@Override
    protected void onSuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, Authentication authResult) throws IOException {
        super.onSuccessfulAuthentication(request,response,authResult);
        // make sure we have a session to store this successful authentication, given that we no longer
        // let HttpSessionContextIntegrationFilter2 to create sessions.
        // HttpSessionContextIntegrationFilter stores the updated SecurityContext object into this session later
        // (either when a redirect is issued, via its HttpResponseWrapper, or when the execution returns to its
        // doFilter method.
        request.getSession().invalidate();
        request.getSession();
    }