private void init(ErrorDispatcher err) throws JasperException {
        if (initialized)
            return;

        String blockExternalString = ctxt.getInitParameter(
                Constants.XML_BLOCK_EXTERNAL_INIT_PARAM);
        boolean blockExternal;
        if (blockExternalString == null) {
            blockExternal = true;
        } else {
            blockExternal = Boolean.parseBoolean(blockExternalString);
        }

        TagPluginParser parser;
        ClassLoader original;
        if (Constants.IS_SECURITY_ENABLED) {
            PrivilegedGetTccl pa = new PrivilegedGetTccl();
            original = AccessController.doPrivileged(pa);
            } else {
                original = Thread.currentThread().getContextClassLoader();
            }
        try {
            if (Constants.IS_SECURITY_ENABLED) {
                PrivilegedSetTccl pa =
                        new PrivilegedSetTccl(JspDocumentParser.class.getClassLoader());
                AccessController.doPrivileged(pa);
            } else {
                Thread.currentThread().setContextClassLoader(
                        JspDocumentParser.class.getClassLoader());
            }

            parser = new TagPluginParser(ctxt, blockExternal);

        } finally {
            if (Constants.IS_SECURITY_ENABLED) {
                PrivilegedSetTccl pa = new PrivilegedSetTccl(original);
                AccessController.doPrivileged(pa);
            } else {
                Thread.currentThread().setContextClassLoader(original);
            }
        }

        try {
            Enumeration<URL> urls =
                    ctxt.getClassLoader().getResources(META_INF_JASPER_TAG_PLUGINS_XML);
            if (urls != null) {
                while (urls.hasMoreElements()) {
                    URL url = urls.nextElement();
                    parser.parse(url);
                }
            }

            URL url = ctxt.getResource(TAG_PLUGINS_XML);
            if (url != null) {
                parser.parse(url);
            }
        } catch (IOException | SAXException e) {
            throw new JasperException(e);
        }

        Map<String, String> plugins = parser.getPlugins();
        tagPlugins = new HashMap<>(plugins.size());
        for (Map.Entry<String, String> entry : plugins.entrySet()) {
            try {
                String tagClass = entry.getKey();
                String pluginName = entry.getValue();
                Class<?> pluginClass = ctxt.getClassLoader().loadClass(pluginName);
                TagPlugin plugin = (TagPlugin) pluginClass.newInstance();
                tagPlugins.put(tagClass, plugin);
            } catch (Exception e) {
                err.jspError(e);
            }
        }
        initialized = true;
    }