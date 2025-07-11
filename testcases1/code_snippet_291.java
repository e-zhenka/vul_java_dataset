private Session connectToServerRecursive(Endpoint endpoint,
            ClientEndpointConfig clientEndpointConfiguration, URI path,
            Set<URI> redirectSet)
            throws DeploymentException {

        boolean secure = false;
        ByteBuffer proxyConnect = null;
        URI proxyPath;

        // Validate scheme (and build proxyPath)
        String scheme = path.getScheme();
        if ("ws".equalsIgnoreCase(scheme)) {
            proxyPath = URI.create("http" + path.toString().substring(2));
        } else if ("wss".equalsIgnoreCase(scheme)) {
            proxyPath = URI.create("https" + path.toString().substring(3));
            secure = true;
        } else {
            throw new DeploymentException(sm.getString(
                    "wsWebSocketContainer.pathWrongScheme", scheme));
        }

        // Validate host
        String host = path.getHost();
        if (host == null) {
            throw new DeploymentException(
                    sm.getString("wsWebSocketContainer.pathNoHost"));
        }
        int port = path.getPort();

        SocketAddress sa = null;

        // Check to see if a proxy is configured. Javadoc indicates return value
        // will never be null
        List<Proxy> proxies = ProxySelector.getDefault().select(proxyPath);
        Proxy selectedProxy = null;
        for (Proxy proxy : proxies) {
            if (proxy.type().equals(Proxy.Type.HTTP)) {
                sa = proxy.address();
                if (sa instanceof InetSocketAddress) {
                    InetSocketAddress inet = (InetSocketAddress) sa;
                    if (inet.isUnresolved()) {
                        sa = new InetSocketAddress(inet.getHostName(), inet.getPort());
                    }
                }
                selectedProxy = proxy;
                break;
            }
        }

        // If the port is not explicitly specified, compute it based on the
        // scheme
        if (port == -1) {
            if ("ws".equalsIgnoreCase(scheme)) {
                port = 80;
            } else {
                // Must be wss due to scheme validation above
                port = 443;
            }
        }

        // If sa is null, no proxy is configured so need to create sa
        if (sa == null) {
            sa = new InetSocketAddress(host, port);
        } else {
            proxyConnect = createProxyRequest(host, port);
        }

        // Create the initial HTTP request to open the WebSocket connection
        Map<String, List<String>> reqHeaders = createRequestHeaders(host, port,
                clientEndpointConfiguration);
        clientEndpointConfiguration.getConfigurator().beforeRequest(reqHeaders);
        if (Constants.DEFAULT_ORIGIN_HEADER_VALUE != null
                && !reqHeaders.containsKey(Constants.ORIGIN_HEADER_NAME)) {
            List<String> originValues = new ArrayList<>(1);
            originValues.add(Constants.DEFAULT_ORIGIN_HEADER_VALUE);
            reqHeaders.put(Constants.ORIGIN_HEADER_NAME, originValues);
        }
        ByteBuffer request = createRequest(path, reqHeaders);

        AsynchronousSocketChannel socketChannel;
        try {
            socketChannel = AsynchronousSocketChannel.open(getAsynchronousChannelGroup());
        } catch (IOException ioe) {
            throw new DeploymentException(sm.getString(
                    "wsWebSocketContainer.asynchronousSocketChannelFail"), ioe);
        }

        Map<String,Object> userProperties = clientEndpointConfiguration.getUserProperties();

        // Get the connection timeout
        long timeout = Constants.IO_TIMEOUT_MS_DEFAULT;
        String timeoutValue = (String) userProperties.get(Constants.IO_TIMEOUT_MS_PROPERTY);
        if (timeoutValue != null) {
            timeout = Long.valueOf(timeoutValue).intValue();
        }

        // Set-up
        // Same size as the WsFrame input buffer
        ByteBuffer response = ByteBuffer.allocate(getDefaultMaxBinaryMessageBufferSize());
        String subProtocol;
        boolean success = false;
        List<Extension> extensionsAgreed = new ArrayList<>();
        Transformation transformation = null;

        // Open the connection
        Future<Void> fConnect = socketChannel.connect(sa);
        AsyncChannelWrapper channel = null;

        if (proxyConnect != null) {
            try {
                fConnect.get(timeout, TimeUnit.MILLISECONDS);
                // Proxy CONNECT is clear text
                channel = new AsyncChannelWrapperNonSecure(socketChannel);
                writeRequest(channel, proxyConnect, timeout);
                HttpResponse httpResponse = processResponse(response, channel, timeout);
                if (httpResponse.getStatus() != 200) {
                    throw new DeploymentException(sm.getString(
                            "wsWebSocketContainer.proxyConnectFail", selectedProxy,
                            Integer.toString(httpResponse.getStatus())));
                }
            } catch (TimeoutException | InterruptedException | ExecutionException |
                    EOFException e) {
                if (channel != null) {
                    channel.close();
                }
                throw new DeploymentException(
                        sm.getString("wsWebSocketContainer.httpRequestFailed"), e);
            }
        }

        if (secure) {
            // Regardless of whether a non-secure wrapper was created for a
            // proxy CONNECT, need to use TLS from this point on so wrap the
            // original AsynchronousSocketChannel
            SSLEngine sslEngine = createSSLEngine(userProperties);
            channel = new AsyncChannelWrapperSecure(socketChannel, sslEngine);
        } else if (channel == null) {
            // Only need to wrap as this point if it wasn't wrapped to process a
            // proxy CONNECT
            channel = new AsyncChannelWrapperNonSecure(socketChannel);
        }

        try {
            fConnect.get(timeout, TimeUnit.MILLISECONDS);

            Future<Void> fHandshake = channel.handshake();
            fHandshake.get(timeout, TimeUnit.MILLISECONDS);

            writeRequest(channel, request, timeout);

            HttpResponse httpResponse = processResponse(response, channel, timeout);

            // Check maximum permitted redirects
            int maxRedirects = Constants.MAX_REDIRECTIONS_DEFAULT;
            String maxRedirectsValue =
                    (String) userProperties.get(Constants.MAX_REDIRECTIONS_PROPERTY);
            if (maxRedirectsValue != null) {
                maxRedirects = Integer.parseInt(maxRedirectsValue);
            }

            if (httpResponse.status != 101) {
                if(isRedirectStatus(httpResponse.status)){
                    List<String> locationHeader =
                            httpResponse.getHandshakeResponse().getHeaders().get(
                                    Constants.LOCATION_HEADER_NAME);

                    if (locationHeader == null || locationHeader.isEmpty() ||
                            locationHeader.get(0) == null || locationHeader.get(0).isEmpty()) {
                        throw new DeploymentException(sm.getString(
                                "wsWebSocketContainer.missingLocationHeader",
                                Integer.toString(httpResponse.status)));
                    }

                    URI redirectLocation = URI.create(locationHeader.get(0)).normalize();

                    if (!redirectLocation.isAbsolute()) {
                        redirectLocation = path.resolve(redirectLocation);
                    }

                    String redirectScheme = redirectLocation.getScheme().toLowerCase();

                    if (redirectScheme.startsWith("http")) {
                        redirectLocation = new URI(redirectScheme.replace("http", "ws"),
                                redirectLocation.getUserInfo(), redirectLocation.getHost(),
                                redirectLocation.getPort(), redirectLocation.getPath(),
                                redirectLocation.getQuery(), redirectLocation.getFragment());
                    }

                    if (!redirectSet.add(redirectLocation) || redirectSet.size() > maxRedirects) {
                        throw new DeploymentException(sm.getString(
                                "wsWebSocketContainer.redirectThreshold", redirectLocation,
                                Integer.toString(redirectSet.size()),
                                Integer.toString(maxRedirects)));
                    }

                    return connectToServerRecursive(endpoint, clientEndpointConfiguration, redirectLocation, redirectSet);

                }

                else if (httpResponse.status == 401) {

                    if (userProperties.get(Constants.AUTHORIZATION_HEADER_NAME) != null) {
                        throw new DeploymentException(sm.getString(
                                "wsWebSocketContainer.failedAuthentication",
                                Integer.valueOf(httpResponse.status)));
                    }

                    List<String> wwwAuthenticateHeaders = httpResponse.getHandshakeResponse()
                            .getHeaders().get(Constants.WWW_AUTHENTICATE_HEADER_NAME);

                    if (wwwAuthenticateHeaders == null || wwwAuthenticateHeaders.isEmpty() ||
                            wwwAuthenticateHeaders.get(0) == null || wwwAuthenticateHeaders.get(0).isEmpty()) {
                        throw new DeploymentException(sm.getString(
                                "wsWebSocketContainer.missingWWWAuthenticateHeader",
                                Integer.toString(httpResponse.status)));
                    }

                    String authScheme = wwwAuthenticateHeaders.get(0).split("\\s+", 2)[0];
                    String requestUri = new String(request.array(), StandardCharsets.ISO_8859_1)
                            .split("\\s", 3)[1];

                    Authenticator auth = AuthenticatorFactory.getAuthenticator(authScheme);

                    if (auth == null) {
                        throw new DeploymentException(
                                sm.getString("wsWebSocketContainer.unsupportedAuthScheme",
                                        Integer.valueOf(httpResponse.status), authScheme));
                    }

                    userProperties.put(Constants.AUTHORIZATION_HEADER_NAME, auth.getAuthorization(
                            requestUri, wwwAuthenticateHeaders.get(0), userProperties));

                    return connectToServerRecursive(endpoint, clientEndpointConfiguration, path, redirectSet);

                }

                else {
                    throw new DeploymentException(sm.getString("wsWebSocketContainer.invalidStatus",
                            Integer.toString(httpResponse.status)));
                }
            }
            HandshakeResponse handshakeResponse = httpResponse.getHandshakeResponse();
            clientEndpointConfiguration.getConfigurator().afterResponse(handshakeResponse);

            // Sub-protocol
            List<String> protocolHeaders = handshakeResponse.getHeaders().get(
                    Constants.WS_PROTOCOL_HEADER_NAME);
            if (protocolHeaders == null || protocolHeaders.size() == 0) {
                subProtocol = null;
            } else if (protocolHeaders.size() == 1) {
                subProtocol = protocolHeaders.get(0);
            } else {
                throw new DeploymentException(
                        sm.getString("wsWebSocketContainer.invalidSubProtocol"));
            }

            // Extensions
            // Should normally only be one header but handle the case of
            // multiple headers
            List<String> extHeaders = handshakeResponse.getHeaders().get(
                    Constants.WS_EXTENSIONS_HEADER_NAME);
            if (extHeaders != null) {
                for (String extHeader : extHeaders) {
                    Util.parseExtensionHeader(extensionsAgreed, extHeader);
                }
            }

            // Build the transformations
            TransformationFactory factory = TransformationFactory.getInstance();
            for (Extension extension : extensionsAgreed) {
                List<List<Extension.Parameter>> wrapper = new ArrayList<>(1);
                wrapper.add(extension.getParameters());
                Transformation t = factory.create(extension.getName(), wrapper, false);
                if (t == null) {
                    throw new DeploymentException(sm.getString(
                            "wsWebSocketContainer.invalidExtensionParameters"));
                }
                if (transformation == null) {
                    transformation = t;
                } else {
                    transformation.setNext(t);
                }
            }

            success = true;
        } catch (ExecutionException | InterruptedException | SSLException |
                EOFException | TimeoutException | URISyntaxException | AuthenticationException e) {
            throw new DeploymentException(
                    sm.getString("wsWebSocketContainer.httpRequestFailed"), e);
        } finally {
            if (!success) {
                channel.close();
            }
        }

        // Switch to WebSocket
        WsRemoteEndpointImplClient wsRemoteEndpointClient = new WsRemoteEndpointImplClient(channel);

        WsSession wsSession = new WsSession(endpoint, wsRemoteEndpointClient,
                this, null, null, null, null, null, extensionsAgreed,
                subProtocol, Collections.<String,String>emptyMap(), secure,
                clientEndpointConfiguration);

        WsFrameClient wsFrameClient = new WsFrameClient(response, channel,
                wsSession, transformation);
        // WsFrame adds the necessary final transformations. Copy the
        // completed transformation chain to the remote end point.
        wsRemoteEndpointClient.setTransformation(wsFrameClient.getTransformation());

        endpoint.onOpen(wsSession, clientEndpointConfiguration);
        registerSession(endpoint, wsSession);

        /* It is possible that the server sent one or more messages as soon as
         * the WebSocket connection was established. Depending on the exact
         * timing of when those messages were sent they could be sat in the
         * input buffer waiting to be read and will not trigger a "data
         * available to read" event. Therefore, it is necessary to process the
         * input buffer here. Note that this happens on the current thread which
         * means that this thread will be used for any onMessage notifications.
         * This is a special case. Subsequent "data available to read" events
         * will be handled by threads from the AsyncChannelGroup's executor.
         */
        wsFrameClient.startInputProcessing();

        return wsSession;
    }