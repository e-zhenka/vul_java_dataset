private void init() {
        authMap = new HashMap<Pair<String, String>, String>();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(getClass().getResource("/" + authorizations).openStream());
            doc.getDocumentElement().normalize();

            Node authNode = null;
            NodeList root = doc.getChildNodes();
            for (int i = 0; i < root.getLength() && authNode == null; i++) {
                if ("auth".equals(root.item(i).getNodeName())) {
                    authNode = root.item(i);
                }
            }
            if (authNode == null) {
                throw new IllegalArgumentException("Could not find root <auth> node");
            }

            NodeList pages = authNode.getChildNodes();
            for (int i = 0; i < pages.getLength(); i++) {
                if ("page".equals(pages.item(i).getNodeName())) {
                    String page = pages.item(i).getAttributes().getNamedItem("id").getTextContent();

                    NodeList actions = pages.item(i).getChildNodes();
                    for (int j = 0; j < actions.getLength(); j++) {
                        if ("action".equals(actions.item(j).getNodeName())) {
                            String action = actions.item(j).getAttributes().getNamedItem("id").getTextContent();

                            NodeList entitlements = actions.item(j).getChildNodes();
                            for (int k = 0; k < entitlements.getLength(); k++) {
                                if ("entitlement".equals(entitlements.item(k).getNodeName())) {
                                    String entitlement = entitlements.item(k).getTextContent();
                                    authMap.put(new ImmutablePair<String, String>(page, action), entitlement);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("While initializing parsing of {}", authorizations, e);
        }
    }