protected Result findResult(String path, String resultCode, String ext, ActionContext actionContext,
                                Map<String, ResultTypeConfig> resultsByExtension) {
        try {
            boolean traceEnabled = LOG.isTraceEnabled();
            if (traceEnabled) {
                LOG.trace("Checking ServletContext for [#0]", path);
            }

            URL resource = servletContext.getResource(path);
            if (resource != null && resource.getPath().endsWith(path)) {
                if (traceEnabled) {
                    LOG.trace("Found resource #0", resource);
                }
                return buildResult(path, resultCode, resultsByExtension.get(ext), actionContext);
            }

            if (traceEnabled) {
                LOG.trace("Checking ClassLoader for #0", path);
            }

            String classLoaderPath = path.startsWith("/") ? path.substring(1, path.length()) : path;
            resource = ClassLoaderUtil.getResource(classLoaderPath, getClass());
            if (resource != null && resource.getPath().endsWith(classLoaderPath)) {
                if (traceEnabled) {
                    LOG.trace("Found resource #0", resource);
                }
                return buildResult(path, resultCode, resultsByExtension.get(ext), actionContext);
            }
        } catch (MalformedURLException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Unable to parse template path: [#0] skipping...", path);
            }
        }

        return null;
    }