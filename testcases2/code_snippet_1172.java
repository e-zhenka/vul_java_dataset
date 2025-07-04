private Document createDocFromMessage(InputStream message)
            throws SAXException, IOException, ParserConfigurationException {
        DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
        //Disabling DTDs in order to avoid XXE xml-based attacks.
        disableFeature(dbfactory, DISALLOW_DTD_FEATURE);
        disableFeature(dbfactory, DISALLOW_EXTERNAL_DTD);
        dbfactory.setXIncludeAware(false);
        dbfactory.setExpandEntityReferences(false);
        DocumentBuilder builder = dbfactory.newDocumentBuilder();
        return builder.parse(new InputSource(message));
    }