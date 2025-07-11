@Override
    protected void configureDataFormat(DataFormat dataFormat, CamelContext camelContext) {
        if (mappingFile != null) {
            setProperty(camelContext, dataFormat, "mappingFile", mappingFile);
        }
        // should be true by default
        boolean isValidation = getValidation() == null || getValidation();
        setProperty(camelContext, dataFormat, "validation", isValidation);

        if (encoding != null) {
            setProperty(camelContext, dataFormat, "encoding", encoding);
        }
        if (packages != null) {
            setProperty(camelContext, dataFormat, "packages", packages);
        }
        if (classes != null) {
            setProperty(camelContext, dataFormat, "classes", classes);
        }
    }