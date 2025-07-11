public void handleMessage(Message message) throws Fault {
        String method = (String)message.get(Message.HTTP_REQUEST_METHOD);
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Invoking HTTP method " + method);
        }
        if (!isGET(message)) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "URIMappingInterceptor can only handle HTTP GET, not HTTP " + method);
            }
            return;
        }

        String opName = getOperationName(message);
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("URIMappingInterceptor get operation: " + opName);
        }
        BindingOperationInfo op = ServiceModelUtil.getOperation(message.getExchange(), opName);
        
        if (op == null || opName == null || op.getName() == null
            || StringUtils.isEmpty(op.getName().getLocalPart())
            || !opName.equals(op.getName().getLocalPart())) {
            
            if (!Boolean.TRUE.equals(message.getContextualProperty(NO_VALIDATE_PARTS))) {
                throw new Fault(new org.apache.cxf.common.i18n.Message("NO_OPERATION_PATH", LOG, opName,
                                                                       message.get(Message.PATH_INFO)));
            }
            MessageContentsList params = new MessageContentsList();
            params.add(null);
            message.setContent(List.class, params);
            if (op == null) {
                op = findAnyOp(message.getExchange());
            }
            if (op != null) {
                message.getExchange().put(BindingOperationInfo.class, op);
            }
        } else {
            message.getExchange().put(BindingOperationInfo.class, op);
            MessageContentsList params = getParameters(message, op);
            message.setContent(List.class, params);
        }
    }