protected boolean evaluate(InputSource inputSource) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            setupFeatures(factory);
            factory.setNamespaceAware(true);
            factory.setIgnoringElementContentWhitespace(true);
            factory.setIgnoringComments(true);
            DocumentBuilder dbuilder = factory.newDocumentBuilder();
            Document doc = dbuilder.parse(inputSource);

            //An XPath expression could return a true or false value instead of a node.
            //eval() is a better way to determine the boolean value of the exp.
            //For compliance with legacy behavior where selecting an empty node returns true,
            //selectNodeIterator is attempted in case of a failure.

            CachedXPathAPI cachedXPathAPI = new CachedXPathAPI();
            XObject result = cachedXPathAPI.eval(doc, xpath);
            if (result.bool())
            	return true;
            else {
            	NodeIterator iterator = cachedXPathAPI.selectNodeIterator(doc, xpath);
            	return (iterator.nextNode() != null);
            }

        } catch (Throwable e) {
            return false;
        }
    }