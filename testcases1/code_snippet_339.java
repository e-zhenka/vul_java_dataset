@Override
    protected void onExchange(Exchange exchange) throws Exception {

        if (!cacheStylesheet || cacheCleared) {
            loadResource(resourceUri);
        }
        super.onExchange(exchange);

    }