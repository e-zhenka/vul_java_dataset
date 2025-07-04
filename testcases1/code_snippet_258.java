protected XStream createXStream(ClassResolver resolver, ClassLoader classLoader) {
        if (xstreamDriver != null) {
            xstream = new XStream(xstreamDriver);
        } else {
            xstream = new XStream();
        }

        if (mode != null) {
            xstream.setMode(getModeFromString(mode));
        }

        ClassLoader xstreamLoader = xstream.getClassLoader();
        if (classLoader != null && xstreamLoader instanceof CompositeClassLoader) {
            ((CompositeClassLoader) xstreamLoader).add(classLoader);
        }

        try {
            if (this.implicitCollections != null) {
                for (Entry<String, String[]> entry : this.implicitCollections.entrySet()) {
                    for (String name : entry.getValue()) {
                        xstream.addImplicitCollection(resolver.resolveMandatoryClass(entry.getKey()), name);
                    }
                }
            }

            if (this.aliases != null) {
                for (Entry<String, String> entry : this.aliases.entrySet()) {
                    xstream.alias(entry.getKey(), resolver.resolveMandatoryClass(entry.getValue()));
                    // It can turn the auto-detection mode off
                    xstream.processAnnotations(resolver.resolveMandatoryClass(entry.getValue()));
                }
            }

            if (this.omitFields != null) {
                for (Entry<String, String[]> entry : this.omitFields.entrySet()) {
                    for (String name : entry.getValue()) {
                        xstream.omitField(resolver.resolveMandatoryClass(entry.getKey()), name);
                    }
                }
            }

            if (this.converters != null) {
                for (String name : this.converters) {
                    Class<Converter> converterClass = resolver.resolveMandatoryClass(name, Converter.class);
                    Converter converter;

                    Constructor<Converter> con = null;
                    try {
                        con = converterClass.getDeclaredConstructor(new Class[]{XStream.class});
                    } catch (Exception e) {
                        //swallow as we null check in a moment.
                    }
                    if (con != null) {
                        converter = con.newInstance(xstream);
                    } else {
                        converter = converterClass.newInstance();
                        try {
                            Method method = converterClass.getMethod("setXStream", new Class[]{XStream.class});
                            if (method != null) {
                                ObjectHelper.invokeMethod(method, converter, xstream);
                            }
                        } catch (Throwable e) {
                            // swallow, as it just means the user never add an XStream setter, which is optional
                        }
                    }

                    xstream.registerConverter(converter);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Unable to build XStream instance", e);
        }

        return xstream;
    }