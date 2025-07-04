private Document parse(String configFile) {
    Document doc = null;
    try {

      SAXParserFactory factory = SAXParserFactory.newInstance();
      factory.setValidating(false);

      // Create the builder and parse the file
      SAXParser parser = factory.newSAXParser();
      parser.parse(configFile, this);

      UIMAFramework.getLogger().log(Level.CONFIG, "Resource::" + getResourceSpecifierPath());
      UIMAFramework.getLogger().log(Level.CONFIG, "Instance Count::" + getInstanceCount());
      UIMAFramework.getLogger().log(Level.CONFIG, "Service Name::" + getServiceName());
      UIMAFramework.getLogger().log(Level.CONFIG, "Filter String::" + getFilterString());
      UIMAFramework.getLogger().log(Level.CONFIG, "Naming Service Host::" + getNamingServiceHost());
      UIMAFramework.getLogger().log(Level.CONFIG,
              "Server Socket Timeout::" + getServerSocketTimeout());

    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return doc;
  }