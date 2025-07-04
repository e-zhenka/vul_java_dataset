@Override
    public void init(ServletConfig config) throws ServletException {

        super.init(config);
        this.config = config;
        this.context = config.getServletContext();

        // Initialize the JSP Runtime Context
        // Check for a custom Options implementation
        String engineOptionsName = config.getInitParameter("engineOptionsClass");
        if (engineOptionsName != null) {
            // Instantiate the indicated Options implementation
            try {
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                Class<?> engineOptionsClass = loader.loadClass(engineOptionsName);
                Class<?>[] ctorSig = { ServletConfig.class, ServletContext.class };
                Constructor<?> ctor = engineOptionsClass.getConstructor(ctorSig);
                Object[] args = { config, context };
                options = (Options) ctor.newInstance(args);
            } catch (Throwable e) {
                e = ExceptionUtils.unwrapInvocationTargetException(e);
                ExceptionUtils.handleThrowable(e);
                // Need to localize this.
                log.warn("Failed to load engineOptionsClass", e);
                // Use the default Options implementation
                options = new EmbeddedServletOptions(config, context);
            }
        } else {
            // Use the default Options implementation
            options = new EmbeddedServletOptions(config, context);
        }
        rctxt = new JspRuntimeContext(context, options);
        if (config.getInitParameter("jspFile") != null) {
            jspFile = config.getInitParameter("jspFile");
            try {
                if (null == context.getResource(jspFile)) {
                    return;
                }
            } catch (MalformedURLException e) {
                throw new ServletException("cannot locate jsp file", e);
            }
            try {
                if (SecurityUtil.isPackageProtectionEnabled()){
                   AccessController.doPrivileged(new PrivilegedExceptionAction<Object>(){
                        @Override
                        public Object run() throws IOException, ServletException {
                            serviceJspFile(null, null, jspFile, true);
                            return null;
                        }
                    });
                } else {
                    serviceJspFile(null, null, jspFile, true);
                }
            } catch (IOException e) {
                throw new ServletException("Could not precompile jsp: " + jspFile, e);
            } catch (PrivilegedActionException e) {
                Throwable t = e.getCause();
                if (t instanceof ServletException) throw (ServletException)t;
                throw new ServletException("Could not precompile jsp: " + jspFile, e);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug(Localizer.getMessage("jsp.message.scratch.dir.is",
                    options.getScratchDir().toString()));
            log.debug(Localizer.getMessage("jsp.message.dont.modify.servlets"));
        }
    }