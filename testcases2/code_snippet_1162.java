protected SchemaFactory createSchemaFactory() {
        SchemaFactory factory = SchemaFactory.newInstance(schemaLanguage);
        if (getResourceResolver() != null) {
            factory.setResourceResolver(getResourceResolver());
        }  
        if (camelContext == null || !Boolean.parseBoolean(camelContext.getProperty(ACCESS_EXTERNAL_DTD))) {
            try {
                factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            } catch (SAXException e) {
                LOG.error(e.getMessage(), e);
                throw new IllegalStateException(e);
            } 
        }
        return factory;
    }