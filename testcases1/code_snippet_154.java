public boolean processSendfile(SelectionKey sk, KeyAttachment attachment, boolean reg, boolean event) {
            NioChannel sc = null;
            try {
                //unreg(sk,attachment);//only do this if we do process send file on a separate thread
                SendfileData sd = attachment.getSendfileData();
                if ( sd.fchannel == null ) {
                    File f = new File(sd.fileName);
                    if ( !f.exists() ) {
                        cancelledKey(sk,SocketStatus.ERROR,false);
                        return false;
                    }
                    sd.fchannel = new FileInputStream(f).getChannel();
                }
                sc = attachment.getChannel();
                sc.setSendFile(true);
                WritableByteChannel wc = ((sc instanceof SecureNioChannel)?sc:sc.getIOChannel());
                
                if (sc.getOutboundRemaining()>0) {
                    if (sc.flushOutbound()) {
                        attachment.access();
                    }
                } else {
                    long written = sd.fchannel.transferTo(sd.pos,sd.length,wc);
                    if ( written > 0 ) {
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
                if ( sd.length <= 0 && sc.getOutboundRemaining()<=0) {
                    if (log.isDebugEnabled()) {
                        log.debug("Send file complete for:"+sd.fileName);
                    }
                    attachment.setSendfileData(null);
                    try {sd.fchannel.close();}catch(Exception ignore){}
                    if ( sd.keepAlive ) {
                        if (reg) {
                            if (log.isDebugEnabled()) {
                                log.debug("Connection is keep alive, registering back for OP_READ");
                            }
                            if (event) {
                                this.add(attachment.getChannel(),SelectionKey.OP_READ);
                            } else {
                                reg(sk,attachment,SelectionKey.OP_READ);
                            }
                        }
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Send file connection is being closed");
                        }
                        cancelledKey(sk,SocketStatus.STOP,false);
                    }
                } else if ( attachment.interestOps() == 0 && reg ) {
                    if (log.isDebugEnabled()) {
                        log.debug("OP_WRITE for sendilfe:"+sd.fileName);
                    }
                    if (event) {
                        add(attachment.getChannel(),SelectionKey.OP_WRITE);
                    } else {
                        reg(sk,attachment,SelectionKey.OP_WRITE);
                    }
                }
            }catch ( IOException x ) {
                if ( log.isDebugEnabled() ) log.debug("Unable to complete sendfile request:", x);
                cancelledKey(sk,SocketStatus.ERROR,false);
                return false;
            }catch ( Throwable t ) {
                log.error("",t);
                cancelledKey(sk, SocketStatus.ERROR, false);
                return false;
            }finally {
                if (sc!=null) sc.setSendFile(false);
            }
            return true;
        }