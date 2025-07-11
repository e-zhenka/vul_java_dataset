public void handleMessage(Message message) {
        if (isGET(message) || message.getContent(XMLStreamReader.class) != null) {
            LOG.fine("StaxInInterceptor skipped.");
            return;
        }
        InputStream is = message.getContent(InputStream.class);
        Reader reader = null;
        if (is == null) {
            reader = message.getContent(Reader.class);
            if (reader == null) {
                return;
            }
        }
        String contentType = (String)message.get(Message.CONTENT_TYPE);
        
        if (contentType != null && contentType.contains("text/html")) {
            String htmlMessage = null;
            try {
                htmlMessage = IOUtils.toString(is, 500);
            } catch (IOException e) {
                throw new Fault(new org.apache.cxf.common.i18n.Message("INVALID_HTML_RESPONSETYPE",
                        LOG, "(none)"));
            }
            throw new Fault(new org.apache.cxf.common.i18n.Message("INVALID_HTML_RESPONSETYPE",
                    LOG, (htmlMessage == null || htmlMessage.length() == 0) ? "(none)" : htmlMessage));
        }
        if (contentType == null) {
            //if contentType is null, this is likely a an empty post/put/delete/similar, lets see if it's
            //detectable at all
            Map<String, List<String>> m = CastUtils.cast((Map<?, ?>)message.get(Message.PROTOCOL_HEADERS));
            if (m != null) {
                List<String> contentLen = HttpHeaderHelper
                    .getHeader(m, HttpHeaderHelper.CONTENT_LENGTH);
                List<String> contentTE = HttpHeaderHelper
                    .getHeader(m, HttpHeaderHelper.CONTENT_TRANSFER_ENCODING);
                if ((StringUtils.isEmpty(contentLen) || "0".equals(contentLen.get(0)))
                    && StringUtils.isEmpty(contentTE)) {
                    return;
                }
            }
        }

        String encoding = (String)message.get(Message.ENCODING);

        XMLStreamReader xreader;
        try {
            XMLInputFactory factory = getXMLInputFactory(message);
            if (factory == null) {
                if (reader != null) {
                    xreader = StaxUtils.createXMLStreamReader(reader);
                } else {
                    xreader = StaxUtils.createXMLStreamReader(is, encoding);
                }
            } else {
                synchronized (factory) {
                    if (reader != null) {
                        xreader = factory.createXMLStreamReader(reader);
                    } else {
                        xreader = factory.createXMLStreamReader(is, encoding);
                    }
                }                
            }
            xreader = configureRestrictions(xreader, message);
        } catch (XMLStreamException e) {
            throw new Fault(new org.apache.cxf.common.i18n.Message("STREAM_CREATE_EXC",
                                                                   LOG,
                                                                   encoding), e);
        }
        message.setContent(XMLStreamReader.class, xreader);
        message.getInterceptorChain().add(StaxInEndingInterceptor.INSTANCE);
    }