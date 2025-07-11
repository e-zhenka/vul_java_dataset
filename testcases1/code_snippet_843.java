public void doDynamic(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        String path = req.getRestOfPath();

        String pathUC = path.toUpperCase(Locale.ENGLISH);
        if (path.isEmpty() || path.contains("..") || path.startsWith(".") || path.contains("%")
                || pathUC.contains("META-INF") || pathUC.contains("WEB-INF")
                // ClassicPluginStrategy#explode produce that file to know if a new explosion is required or not
                || pathUC.equals("/.TIMESTAMP2")
        ) {
            LOGGER.warning("rejecting possibly malicious " + req.getRequestURIWithQueryString());
            rsp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // Stapler routes requests like the "/static/.../foo/bar/zot" to be treated like "/foo/bar/zot"
        // and this is used to serve long expiration header, by using Jenkins.VERSION_HASH as "..."
        // to create unique URLs. Recognize that and set a long expiration header.
        String requestPath = req.getRequestURI().substring(req.getContextPath().length());
        boolean staticLink = requestPath.startsWith("/static/");

        long expires = staticLink ? TimeUnit2.DAYS.toMillis(365) : -1;

        // use serveLocalizedFile to support automatic locale selection
        rsp.serveLocalizedFile(req, new URL(wrapper.baseResourceURL, '.' + path), expires);
    }