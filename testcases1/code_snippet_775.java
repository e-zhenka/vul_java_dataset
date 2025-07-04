@RequirePOST
        public FormValidation doValidateProxy(
                @QueryParameter("testUrl") String testUrl, @QueryParameter("name") String name, @QueryParameter("port") int port,
                @QueryParameter("userName") String userName, @QueryParameter("password") String password,
                @QueryParameter("noProxyHost") String noProxyHost) {

            if (Util.fixEmptyAndTrim(testUrl) == null) {
                return FormValidation.error(Messages.ProxyConfiguration_TestUrlRequired());
            }

            String host = testUrl;
            try {
                URL url = new URL(testUrl);
                host = url.getHost();
            } catch (MalformedURLException e) {
                return FormValidation.error(Messages.ProxyConfiguration_MalformedTestUrl(testUrl));
            }

            GetMethod method = null;
            try {
                method = new GetMethod(testUrl);
                method.getParams().setParameter("http.socket.timeout", DEFAULT_CONNECT_TIMEOUT_MILLIS > 0 ? DEFAULT_CONNECT_TIMEOUT_MILLIS : new Integer(30 * 1000));
                
                HttpClient client = new HttpClient();
                if (Util.fixEmptyAndTrim(name) != null && !isNoProxyHost(host, noProxyHost)) {
                    client.getHostConfiguration().setProxy(name, port);
                    Credentials credentials = createCredentials(userName, password);
                    AuthScope scope = new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT);
                    client.getState().setProxyCredentials(scope, credentials);
                }
                
                int code = client.executeMethod(method);
                if (code != HttpURLConnection.HTTP_OK) {
                    return FormValidation.error(Messages.ProxyConfiguration_FailedToConnect(testUrl, code));
                }
            } catch (IOException e) {
                return FormValidation.error(e, Messages.ProxyConfiguration_FailedToConnectViaProxy(testUrl));
            } finally {
                if (method != null) {
                    method.releaseConnection();
                }
            }
            
            return FormValidation.ok(Messages.ProxyConfiguration_Success());
        }