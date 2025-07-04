private void close() {
        connectionState.set(ConnectionState.CLOSED);
        for (Stream stream : streams.values()) {
            // The connection is closing. Close the associated streams as no
            // longer required.
            stream.receiveReset(Http2Error.CANCEL.getCode());
        }
        try {
            socketWrapper.close();
        } catch (IOException ioe) {
            log.debug(sm.getString("upgradeHandler.socketCloseFailed"), ioe);
        }
    }