@Override
    protected void handleMessage(Http1xServerConnection conn, ContextImpl context, ChannelHandlerContext chctx, Object msg) throws Exception {
      Channel ch = chctx.channel();
      if (msg instanceof HttpRequest) {
        final HttpRequest request = (HttpRequest) msg;

        if (log.isTraceEnabled()) log.trace("Server received request: " + request.getUri());

        if (request.headers().contains(io.vertx.core.http.HttpHeaders.UPGRADE, io.vertx.core.http.HttpHeaders.WEBSOCKET, true)) {

          // As a fun part, Firefox 6.0.2 supports Websockets protocol '7'. But,
          // it doesn't send a normal 'Connection: Upgrade' header. Instead it
          // sends: 'Connection: keep-alive, Upgrade'. Brilliant.
          String connectionHeader = request.headers().get(io.vertx.core.http.HttpHeaders.CONNECTION);
          if (connectionHeader == null || !connectionHeader.toLowerCase().contains("upgrade")) {
            handshakeErrorStatus = BAD_REQUEST;
            handshakeErrorMsg = "\"Connection\" must be \"Upgrade\".";
            return;
          }

          if (request.getMethod() != HttpMethod.GET) {
            handshakeErrorStatus = METHOD_NOT_ALLOWED;
            sendError(null, METHOD_NOT_ALLOWED, ch);
            return;
          }

          if (wsRequest == null) {
            if (request instanceof FullHttpRequest) {
              handshake(conn, (FullHttpRequest) request, ch, chctx);
            } else {
              wsRequest = new DefaultFullHttpRequest(request.getProtocolVersion(), request.getMethod(), request.getUri());
              wsRequest.headers().set(request.headers());
            }
          }
        } else {
          //HTTP request
          conn.handleMessage(msg);
        }
      } else if (msg instanceof WebSocketFrameInternal) {
        //Websocket frame
        WebSocketFrameInternal wsFrame = (WebSocketFrameInternal) msg;
        switch (wsFrame.type()) {
          case BINARY:
          case CONTINUATION:
          case TEXT:
          case PONG:
            conn.handleMessage(msg);
            break;
          case PING:
            // Echo back the content of the PING frame as PONG frame as specified in RFC 6455 Section 5.5.2
            conn.channel().writeAndFlush(new PongWebSocketFrame(wsFrame.getBinaryData().copy()));
            break;
          case CLOSE:
            if (!closeFrameSent) {
              // Echo back close frame and close the connection once it was written.
              // This is specified in the WebSockets RFC 6455 Section  5.4.1
              CloseWebSocketFrame closeFrame = new CloseWebSocketFrame(wsFrame.closeStatusCode(), wsFrame.closeReason());
              ch.writeAndFlush(closeFrame).addListener(ChannelFutureListener.CLOSE);
              closeFrameSent = true;
            }
            conn.handleMessage(msg);
            break;
          default:
            throw new IllegalStateException("Invalid type: " + wsFrame.type());
        }
      } else if (msg instanceof HttpContent) {
        if (wsRequest != null) {
          ByteBuf content = wsRequest.content();
          boolean overflow = content.readableBytes() > 8192;
          content.writeBytes(((HttpContent) msg).content());
          if (content.readableBytes() > 8192) {
            if (!overflow) {
              FullHttpResponse resp = new DefaultFullHttpResponse(
                io.netty.handler.codec.http.HttpVersion.HTTP_1_1,
                HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE
              );
              chctx.writeAndFlush(resp);
              chctx.close();
            }
            if (msg instanceof LastHttpContent) {
              wsRequest = null;
              return;
            }
          }
          if (msg instanceof LastHttpContent) {
            FullHttpRequest req = wsRequest;
            wsRequest = null;
            handshake(conn, req, ch, chctx);
            return;
          }
        } else if (handshakeErrorStatus != null) {
          if (msg instanceof LastHttpContent) {
            sendError(handshakeErrorMsg, handshakeErrorStatus, ch);
            handshakeErrorMsg = null;
            handshakeErrorMsg = null;
          }
          return;
        }
        conn.handleMessage(msg);
      } else {
        throw new IllegalStateException("Invalid message " + msg);
      }
    }