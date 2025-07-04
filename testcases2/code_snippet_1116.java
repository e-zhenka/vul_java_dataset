public void handleMessage(Message message) {
        if (isGET(message) && message.getContent(List.class) != null) {
            LOG.fine("DocLiteralInInterceptor skipped in HTTP GET method");
            return;
        }

        DepthXMLStreamReader xmlReader = getXMLStreamReader(message);
        DataReader<XMLStreamReader> dr = getDataReader(message);
        MessageContentsList parameters = new MessageContentsList();

        Exchange exchange = message.getExchange();
        BindingOperationInfo bop = exchange.getBindingOperationInfo();

        boolean client = isRequestor(message);

        //if body is empty and we have BindingOperationInfo, we do not need to match 
        //operation anymore, just return
        if (bop != null && !StaxUtils.toNextElement(xmlReader)) {
            // body may be empty for partial response to decoupled request
            return;
        }

        //bop might be a unwrapped, wrap it back so that we can get correct info 
        if (bop != null && bop.isUnwrapped()) {
            bop = bop.getWrappedOperation();
        }

        if (bop == null) {
            QName startQName = xmlReader == null 
                ? new QName("http://cxf.apache.org/jaxws/provider", "invoke")
                : xmlReader.getName();
            bop = getBindingOperationInfo(exchange, startQName, client);
        }

        try {
            if (bop != null && bop.isUnwrappedCapable()) {
                ServiceInfo si = bop.getBinding().getService();
                // Wrapped case
                MessageInfo msgInfo = setMessage(message, bop, client, si);
    
                // Determine if we should keep the parameters wrapper
                if (shouldWrapParameters(msgInfo, message)) {
                    QName startQName = xmlReader.getName();
                    if (!msgInfo.getMessageParts().get(0).getConcreteName().equals(startQName)) {
                        throw new Fault("UNEXPECTED_WRAPPER_ELEMENT", LOG, null, startQName,
                                        msgInfo.getMessageParts().get(0).getConcreteName());
                    }
                    Object wrappedObject = dr.read(msgInfo.getMessageParts().get(0), xmlReader);
                    parameters.put(msgInfo.getMessageParts().get(0), wrappedObject);
                } else {
                    // Unwrap each part individually if we don't have a wrapper
    
                    bop = bop.getUnwrappedOperation();
    
                    msgInfo = setMessage(message, bop, client, si);
                    List<MessagePartInfo> messageParts = msgInfo.getMessageParts();
                    Iterator<MessagePartInfo> itr = messageParts.iterator();
    
                    // advance just past the wrapped element so we don't get
                    // stuck
                    if (xmlReader.getEventType() == XMLStreamConstants.START_ELEMENT) {
                        StaxUtils.nextEvent(xmlReader);
                    }
    
                    // loop through each child element
                    getPara(xmlReader, dr, parameters, itr, message);
                }
    
            } else {
                //Bare style
                BindingMessageInfo msgInfo = null;

    
                Endpoint ep = exchange.get(Endpoint.class);
                ServiceInfo si = ep.getEndpointInfo().getService();
                if (bop != null) { //for xml binding or client side
                    if (client) {
                        msgInfo = bop.getOutput();
                    } else {
                        msgInfo = bop.getInput();
                        if (bop.getOutput() == null) {
                            exchange.setOneWay(true);
                        }
                    }
                    if (msgInfo == null) {
                        return;
                    }
                    setMessage(message, bop, client, si, msgInfo.getMessageInfo());
                }
    
                Collection<OperationInfo> operations = null;
                operations = new ArrayList<OperationInfo>();
                operations.addAll(si.getInterface().getOperations());
    
                if (xmlReader == null || !StaxUtils.toNextElement(xmlReader)) {
                    // empty input
    
                    // TO DO : check duplicate operation with no input
                    for (OperationInfo op : operations) {
                        MessageInfo bmsg = op.getInput();
                        if (bmsg.getMessageParts().size() == 0) {
                            BindingOperationInfo boi = ep.getEndpointInfo().getBinding().getOperation(op);
                            exchange.put(BindingOperationInfo.class, boi);
                            exchange.put(OperationInfo.class, op);
                            exchange.setOneWay(op.isOneWay());
                        }
                    }
                    return;
                }
    
                int paramNum = 0;
    
                do {
                    QName elName = xmlReader.getName();
                    Object o = null;
    
                    MessagePartInfo p;
                    if (!client && msgInfo != null && msgInfo.getMessageParts() != null 
                        && msgInfo.getMessageParts().size() == 0) {
                        //no input messagePartInfo
                        return;
                    }
                    
                    if (msgInfo != null && msgInfo.getMessageParts() != null 
                        && msgInfo.getMessageParts().size() > 0) {
                        if (msgInfo.getMessageParts().size() > paramNum) {
                            p = msgInfo.getMessageParts().get(paramNum);
                        } else {
                            p = null;
                        }
                    } else {
                        p = findMessagePart(exchange, operations, elName, client, paramNum, message);
                    }
    
                    if (p == null) {
                        throw new Fault(new org.apache.cxf.common.i18n.Message("NO_PART_FOUND", LOG, elName),
                                        Fault.FAULT_CODE_CLIENT);
                    }
    
                    o = dr.read(p, xmlReader);
                    if (Boolean.TRUE.equals(si.getProperty("soap.force.doclit.bare")) 
                        && parameters.isEmpty()) {
                        // webservice provider does not need to ensure size
                        parameters.add(o);
                    } else {
                        parameters.put(p, o);
                    }
                    
                    paramNum++;
                    if (message.getContent(XMLStreamReader.class) == null || o == xmlReader) {
                        xmlReader = null;
                    }
                } while (xmlReader != null && StaxUtils.toNextElement(xmlReader));
    
            }
    
            message.setContent(List.class, parameters);
        } catch (Fault f) {
            if (!isRequestor(message)) {
                f.setFaultCode(Fault.FAULT_CODE_CLIENT);
            }
            throw f;
        }
    }