@Override
    protected void onFullPongMessage(final WebSocketChannel webSocketChannel, BufferedBinaryMessage bufferedBinaryMessage) {
        if(session.isSessionClosed()) {
            //to bad, the channel has already been closed
            //we just ignore messages that are received after we have closed, as the endpoint is no longer in a valid state to deal with them
            //this this should only happen if a message was on the wire when we called close()
            bufferedBinaryMessage.getData().free();
            return;
        }
        final HandlerWrapper handler = getHandler(FrameType.PONG);
        if (handler != null) {
            final Pooled<ByteBuffer[]> pooled = bufferedBinaryMessage.getData();
            final PongMessage message = DefaultPongMessage.create(toBuffer(pooled.getResource()));

            session.getContainer().invokeEndpointMethod(executor, new Runnable() {
                @Override
                public void run() {
                    try {
                        ((MessageHandler.Whole) handler.getHandler()).onMessage(message);
                    } catch (Exception e) {
                        invokeOnError(e);
                    } finally {
                        pooled.close();
                    }
                }
            });
        }
    }