public void execute(ActionInvocation invocation) throws Exception {
        ValueStack stack = ActionContext.getContext().getValueStack();
        String finalNamespace = this.namespace != null
            ? TextParseUtil.translateVariables(namespace, stack)
            : invocation.getProxy().getNamespace();
        String finalActionName = TextParseUtil.translateVariables(actionName, stack);
        String finalMethodName = this.methodName != null
            ? TextParseUtil.translateVariables(this.methodName, stack)
            : null;

        if (isInChainHistory(finalNamespace, finalActionName, finalMethodName)) {
            addToHistory(finalNamespace, finalActionName, finalMethodName);
            throw new XWorkException("Infinite recursion detected: "
                    + ActionChainResult.getChainHistory().toString());
        }

        if (ActionChainResult.getChainHistory().isEmpty() && invocation != null && invocation.getProxy() != null) {
            addToHistory(finalNamespace, invocation.getProxy().getActionName(), invocation.getProxy().getMethod());
        }
        addToHistory(finalNamespace, finalActionName, finalMethodName);

        HashMap<String, Object> extraContext = new HashMap<String, Object>();
        extraContext.put(ActionContext.VALUE_STACK, ActionContext.getContext().getValueStack());
        extraContext.put(ActionContext.PARAMETERS, ActionContext.getContext().getParameters());
        extraContext.put(CHAIN_HISTORY, ActionChainResult.getChainHistory());

        if (LOG.isDebugEnabled()) {
            LOG.debug("Chaining to action " + finalActionName);
        }

        proxy = actionProxyFactory.createActionProxy(finalNamespace, finalActionName, finalMethodName, extraContext);
        proxy.execute();
    }