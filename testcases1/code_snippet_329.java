private static Document loadConfigFile(SolrResourceLoader resourceLoader, String parseContextConfigLoc) throws Exception {
    return SafeXMLParsing.parseConfigXML(log, resourceLoader, parseContextConfigLoc);
  }