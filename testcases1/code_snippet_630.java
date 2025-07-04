@Override
    protected void configureDataFormat(DataFormat dataFormat, CamelContext camelContext) {
        if (xmlMapper != null) {
            // must be a reference value
            String ref = xmlMapper.startsWith("#") ? xmlMapper : "#" + xmlMapper;
            setProperty(camelContext, dataFormat, "xmlMapper", ref);
        }
        if (unmarshalType != null) {
            setProperty(camelContext, dataFormat, "unmarshalType", unmarshalType);
        }
        if (prettyPrint != null) {
            setProperty(camelContext, dataFormat, "prettyPrint", prettyPrint);
        }
        if (jsonView != null) {
            setProperty(camelContext, dataFormat, "jsonView", jsonView);
        }
        if (include != null) {
            setProperty(camelContext, dataFormat, "include", include);
        }
        if (allowJmsType != null) {
            setProperty(camelContext, dataFormat, "allowJmsType", allowJmsType);
        }
        if (collectionType != null) {
            setProperty(camelContext, dataFormat, "collectionType", collectionType);
        }
        if (useList != null) {
            setProperty(camelContext, dataFormat, "useList", useList);
        }
        if (enableJaxbAnnotationModule != null) {
            setProperty(camelContext, dataFormat, "enableJaxbAnnotationModule", enableJaxbAnnotationModule);
        }
        if (moduleClassNames != null) {
            setProperty(camelContext, dataFormat, "modulesClassNames", moduleClassNames);
        }
        if (moduleRefs != null) {
            setProperty(camelContext, dataFormat, "moduleRefs", moduleRefs);
        }
        if (enableFeatures != null) {
            setProperty(camelContext, dataFormat, "enableFeatures", enableFeatures);
        }
        if (disableFeatures != null) {
            setProperty(camelContext, dataFormat, "disableFeatures", disableFeatures);
        }
    }