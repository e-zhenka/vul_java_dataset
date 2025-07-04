@Override
    protected void connectInternal() throws SmackException, IOException, XMPPException {
        // Establishes the TCP connection to the server and does setup the reader and writer. Throws an exception if
        // there is an error establishing the connection
        connectUsingConfiguration();

        // We connected successfully to the servers TCP port
        socketClosed = false;
        initConnection();

        // Wait with SASL auth until the SASL mechanisms have been received
        saslFeatureReceived.checkIfSuccessOrWaitOrThrow();

        // If TLS is required but the server doesn't offer it, disconnect
        // from the server and throw an error. First check if we've already negotiated TLS
        // and are secure, however (features get parsed a second time after TLS is established).
        if (!isSecureConnection() && getConfiguration().getSecurityMode() == SecurityMode.required) {
            shutdown();
            throw new SecurityRequiredByClientException();
        }

        // Make note of the fact that we're now connected.
        connected = true;
        callConnectionConnectedListener();

        // Automatically makes the login if the user was previously connected successfully
        // to the server and the connection was terminated abruptly
        if (wasAuthenticated) {
            login();
            notifyReconnection();
        }
    }