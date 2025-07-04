public SendfileState processSendfile(SelectionKey sk, KeyAttachment attachment,
                boolean calledByProcessor) {
            NioChannel sc = null;
            try {
                unreg(sk, attachment, sk.readyOps());
                SendfileData sd = attachment.getSendfileData();

                if (log.isTraceEnabled()) {
                    log.trace("Processing send file for: " + sd.fileName);
                }

                if (sd.fchannel == null) {
                    // Setup the file channel
                    File f = new File(sd.fileName);
                    if (!f.exists()) {
                        cancelledKey(sk,SocketStatus.ERROR);
                        return SendfileState.ERROR;
                    }
                    @SuppressWarnings("resource") // Closed when channel is closed
                    FileInputStream fis = new FileInputStream(f);
                    sd.fchannel = fis.getChannel();
                }

                // Configure output channel
                sc = attachment.getSocket();
                // TLS/SSL channel is slightly different
                WritableByteChannel wc = ((sc instanceof SecureNioChannel)?sc:sc.getIOChannel());

                // We still have data in the buffer
                if (sc.getOutboundRemaining()>0) {
                    if (sc.flushOutbound()) {
                        attachment.access();
                    }
                } else {
                    long written = sd.fchannel.transferTo(sd.pos,sd.length,wc);
                    if (written > 0) {
                        sd.pos += written;
                        sd.length -= written;
                        attachment.access();
                    } else {
                        // Unusual not to be able to transfer any bytes
                        // Check the length was set correctly
                        if (sd.fchannel.size() <= sd.pos) {
                            throw new IOException("Sendfile configured to " +
                                    "send more data than was available");
                        }
                    }
                }
                if (sd.length <= 0 && sc.getOutboundRemaining()<=0) {
                    if (log.isDebugEnabled()) {
                        log.debug("Send file complete for: "+sd.fileName);
                    }
                    attachment.setSendfileData(null);
                    try {
                        sd.fchannel.close();
                    } catch (Exception ignore) {
                    }
                    // For calls from outside the Poller, the caller is
                    // responsible for registering the socket for the
                    // appropriate event(s) if sendfile completes.
                    if (!calledByProcessor) {
                        if (sd.keepAlive) {
                            if (log.isDebugEnabled()) {
                                log.debug("Connection is keep alive, registering back for OP_READ");
                            }
                            reg(sk,attachment,SelectionKey.OP_READ);
                        } else {
                            if (log.isDebugEnabled()) {
                                log.debug("Send file connection is being closed");
                            }
                            cancelledKey(sk,SocketStatus.STOP);
                        }
                    }
                    return SendfileState.DONE;
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("OP_WRITE for sendfile: " + sd.fileName);
                    }
                    if (calledByProcessor) {
                        add(attachment.getSocket(),SelectionKey.OP_WRITE);
                    } else {
                        reg(sk,attachment,SelectionKey.OP_WRITE);
                    }
                    return SendfileState.PENDING;
                }
            }catch ( IOException x ) {
                if ( log.isDebugEnabled() ) log.debug("Unable to complete sendfile request:", x);
                cancelledKey(sk,SocketStatus.ERROR);
                return SendfileState.ERROR;
            }catch ( Throwable t ) {
                log.error("",t);
                cancelledKey(sk, SocketStatus.ERROR);
                return SendfileState.ERROR;
            }
        }