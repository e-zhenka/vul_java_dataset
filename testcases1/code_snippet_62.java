private Document parseXML(InputStream pXmlFile) throws ParserException {
    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setValidating(false);
    try {
      dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    } catch (ParserConfigurationException e) {
      // ignore since all implementations are required to support the
      // {@link javax.xml.XMLConstants#FEATURE_SECURE_PROCESSING} feature
    }
    final DocumentBuilder db;
    try {
      db = dbf.newDocumentBuilder();
    } catch (Exception se) {
      throw new ParserException("XML Parser configuration error.", se);
    }
    try {
      db.setEntityResolver(getEntityResolver());
      db.setErrorHandler(getErrorHandler());
      return db.parse(pXmlFile);
    } catch (Exception se) {
      throw new ParserException("Error parsing XML stream: " + se, se);
    }
  }