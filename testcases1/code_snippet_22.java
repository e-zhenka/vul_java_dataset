@Override
    protected void initializeStreams() throws IOException {
        NIOOutputStream outputStream = null;
        try {
            channel = socket.getChannel();
            channel.configureBlocking(false);

            if (sslContext == null) {
                sslContext = SSLContext.getDefault();
            }

            String remoteHost = null;
            int remotePort = -1;

            try {
                URI remoteAddress = new URI(this.getRemoteAddress());
                remoteHost = remoteAddress.getHost();
                remotePort = remoteAddress.getPort();
            } catch (Exception e) {
            }

            // initialize engine, the initial sslSession we get will need to be
            // updated once the ssl handshake process is completed.
            if (remoteHost != null && remotePort != -1) {
                sslEngine = sslContext.createSSLEngine(remoteHost, remotePort);
            } else {
                sslEngine = sslContext.createSSLEngine();
            }

            sslEngine.setUseClientMode(false);
            if (enabledCipherSuites != null) {
                sslEngine.setEnabledCipherSuites(enabledCipherSuites);
            }

            if (enabledProtocols != null) {
                sslEngine.setEnabledProtocols(enabledProtocols);
            }

            if (wantClientAuth) {
                sslEngine.setWantClientAuth(wantClientAuth);
            }

            if (needClientAuth) {
                sslEngine.setNeedClientAuth(needClientAuth);
            }

            sslSession = sslEngine.getSession();

            inputBuffer = ByteBuffer.allocate(sslSession.getPacketBufferSize());
            inputBuffer.clear();

            outputStream = new NIOOutputStream(channel);
            outputStream.setEngine(sslEngine);
            this.dataOut = new DataOutputStream(outputStream);
            this.buffOut = outputStream;
            sslEngine.beginHandshake();
            handshakeStatus = sslEngine.getHandshakeStatus();
            doHandshake();

        } catch (Exception e) {
            try {
                if(outputStream != null) {
                    outputStream.close();
                }
                super.closeStreams();
            } catch (Exception ex) {}
            throw new IOException(e);
        }
    }