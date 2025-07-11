@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)
				|| getNodesCollector().isMonitoringDisabled()) {
			super.doFilter(request, response, chain);
			return;
		}
		final HttpServletRequest httpRequest = (HttpServletRequest) request;

		final String requestURI = httpRequest.getRequestURI();
		final String monitoringUrl = getMonitoringUrl(httpRequest);
		final String monitoringSlavesUrl = monitoringUrl + "/nodes";
		if (!PLUGIN_AUTHENTICATION_DISABLED
				&& (requestURI.equals(monitoringUrl) || requestURI.startsWith(monitoringSlavesUrl))) {
			// only the Hudson/Jenkins administrator can view the monitoring report
			Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
            Enumeration<?> parameterNames = request.getParameterNames();
            while (parameterNames.hasMoreElements()) {
                String parameterName = (String) parameterNames.nextElement();
                for (String value : request.getParameterValues(parameterName)) {
                    if (value.indexOf('"') != -1 || value.indexOf('\'') != -1 || value.indexOf('<') != -1 || value.indexOf('&') != -1) {
                        ((HttpServletResponse) response).sendError(HttpServletResponse.SC_BAD_REQUEST);
                        return;
                    }
                }
            }
		}

		if (requestURI.startsWith(monitoringSlavesUrl)) {
			final String nodeName;
			if (requestURI.equals(monitoringSlavesUrl)) {
				nodeName = null;
			} else {
				nodeName = requestURI.substring(monitoringSlavesUrl.length()).replace("/", "");
			}
			final HttpServletResponse httpResponse = (HttpServletResponse) response;
			doMonitoring(httpRequest, httpResponse, nodeName);
			return;
		}

		super.doFilter(request, response, chain);
	}