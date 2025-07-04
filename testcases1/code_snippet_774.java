private void doInclude(HttpServletRequest request, HttpServletResponse response, String resourceUrl)
			throws ServletException, IOException {

		if (this.contentType != null) {
			response.setContentType(this.contentType);
		}
		String[] resourceUrls = StringUtils.tokenizeToStringArray(resourceUrl, RESOURCE_URL_DELIMITERS);
		for (String url : resourceUrls) {
			String path = StringUtils.cleanPath(url);
			// Check whether URL matches allowed resources
			if (this.allowedResources != null && !this.pathMatcher.match(this.allowedResources, path)) {
				throw new ServletException("Resource [" + path +
						"] does not match allowed pattern [" + this.allowedResources + "]");
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Including resource [" + path + "]");
			}
			RequestDispatcher rd = request.getRequestDispatcher(path);
			rd.include(request, response);
		}
	}