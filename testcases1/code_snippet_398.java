@Override
    public void invoke(Request request, Response response)
        throws IOException, ServletException {

        if (log.isDebugEnabled())
            log.debug("Security checking request " +
                request.getMethod() + " " + request.getRequestURI());
        LoginConfig config = this.context.getLoginConfig();

        // Have we got a cached authenticated Principal to record?
        if (cache) {
            Principal principal = request.getUserPrincipal();
            if (principal == null) {
                Session session = request.getSessionInternal(false);
                if (session != null) {
                    principal = session.getPrincipal();
                    if (principal != null) {
                        if (log.isDebugEnabled())
                            log.debug("We have cached auth type " +
                                session.getAuthType() +
                                " for principal " +
                                session.getPrincipal());
                        request.setAuthType(session.getAuthType());
                        request.setUserPrincipal(principal);
                    }
                }
            }
        }

        // Special handling for form-based logins to deal with the case
        // where the login form (and therefore the "j_security_check" URI
        // to which it submits) might be outside the secured area
        String contextPath = this.context.getPath();
        String requestURI = request.getDecodedRequestURI();
        if (requestURI.startsWith(contextPath) &&
            requestURI.endsWith(Constants.FORM_ACTION)) {
            if (!authenticate(request, response, config)) {
                if (log.isDebugEnabled())
                    log.debug(" Failed authenticate() test ??" + requestURI );
                return;
            }
        }

        // The Servlet may specify security constraints through annotations.
        // Ensure that they have been processed before constraints are checked
        Wrapper wrapper = (Wrapper) request.getMappingData().wrapper; 
        if (wrapper.getServlet() == null) {
            wrapper.load();
        }

        Realm realm = this.context.getRealm();
        // Is this request URI subject to a security constraint?
        SecurityConstraint [] constraints
            = realm.findSecurityConstraints(request, this.context);
       
        if ((constraints == null) /* &&
            (!Constants.FORM_METHOD.equals(config.getAuthMethod())) */ ) {
            if (log.isDebugEnabled())
                log.debug(" Not subject to any constraint");
            getNext().invoke(request, response);
            return;
        }

        // Make sure that constrained resources are not cached by web proxies
        // or browsers as caching can provide a security hole
        if (disableProxyCaching && 
            // FIXME: Disabled for Mozilla FORM support over SSL 
            // (improper caching issue)
            //!request.isSecure() &&
            !"POST".equalsIgnoreCase(request.getMethod())) {
            if (securePagesWithPragma) {
                // FIXME: These cause problems with downloading office docs
                // from IE under SSL and may not be needed for newer Mozilla
                // clients.
                response.setHeader("Pragma", "No-cache");
                response.setHeader("Cache-Control", "no-cache");
            } else {
                response.setHeader("Cache-Control", "private");
            }
            response.setHeader("Expires", DATE_ONE);
        }

        int i;
        // Enforce any user data constraint for this security constraint
        if (log.isDebugEnabled()) {
            log.debug(" Calling hasUserDataPermission()");
        }
        if (!realm.hasUserDataPermission(request, response,
                                         constraints)) {
            if (log.isDebugEnabled()) {
                log.debug(" Failed hasUserDataPermission() test");
            }
            /*
             * ASSERT: Authenticator already set the appropriate
             * HTTP status code, so we do not have to do anything special
             */
            return;
        }

        // Since authenticate modifies the response on failure,
        // we have to check for allow-from-all first.
        boolean authRequired = true;
        for(i=0; i < constraints.length && authRequired; i++) {
            if(!constraints[i].getAuthConstraint()) {
                authRequired = false;
            } else if(!constraints[i].getAllRoles()) {
                String [] roles = constraints[i].findAuthRoles();
                if(roles == null || roles.length == 0) {
                    authRequired = false;
                }
            }
        }
             
        if(authRequired) {  
            if (log.isDebugEnabled()) {
                log.debug(" Calling authenticate()");
            }
            if (!authenticate(request, response, config)) {
                if (log.isDebugEnabled()) {
                    log.debug(" Failed authenticate() test");
                }
                /*
                 * ASSERT: Authenticator already set the appropriate
                 * HTTP status code, so we do not have to do anything
                 * special
                 */
                return;
            } 
            
        }
    
        if (log.isDebugEnabled()) {
            log.debug(" Calling accessControl()");
        }
        if (!realm.hasResourcePermission(request, response,
                                         constraints,
                                         this.context)) {
            if (log.isDebugEnabled()) {
                log.debug(" Failed accessControl() test");
            }
            /*
             * ASSERT: AccessControl method has already set the
             * appropriate HTTP status code, so we do not have to do
             * anything special
             */
            return;
        }
    
        // Any and all specified constraints have been satisfied
        if (log.isDebugEnabled()) {
            log.debug(" Successfully passed all security constraints");
        }
        getNext().invoke(request, response);

    }