private void cacheInput(Message outMessage) {
        if (outMessage.getExchange() == null) {
            return;
        }
        Message inMessage = outMessage.getExchange().getInMessage();
        if (inMessage == null) {
            return;
        }
        Object o = inMessage.get("cxf.io.cacheinput");
        DelegatingInputStream in = inMessage.getContent(DelegatingInputStream.class);
        if (MessageUtils.isTrue(o)) {
            Collection<Attachment> atts = inMessage.getAttachments();
            if (atts != null) {
                for (Attachment a : atts) {
                    if (a.getDataHandler().getDataSource() instanceof AttachmentDataSource) {
                        try {
                            ((AttachmentDataSource)a.getDataHandler().getDataSource()).cache(inMessage);
                        } catch (IOException e) {
                            throw new Fault(e);
                        }
                    }
                }
            }
            if (in != null) {
                in.cacheInput();
            }
        } else if (in != null) {
            //We don't need to cache it, but we may need to consume it in order for the client 
            // to be able to receive a response. (could be blocked sending)
            //However, also don't want to consume indefinitely.   We'll limit to 16M.
            try {
                IOUtils.consume(in, 16 * 1024 * 1024);
            } catch (IOException ioe) {
                //ignore
            }
        }
    }