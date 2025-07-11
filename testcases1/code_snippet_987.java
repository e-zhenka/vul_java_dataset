private static void processHeaderConfig(MultivaluedMap<String, String> httpHeaders, Object object, String key, String prefix) {

        try {
            String property = StringUtils.removeStart(key, prefix);
            String setter = property;
            setter = "set"+setter.substring(0,1).toUpperCase(Locale.US)+setter.substring(1);
            Field field = object.getClass().getDeclaredField(StringUtils.uncapitalize(property));
            //default assume string class
            //if there's a more specific type, e.g. double, int, boolean
            //try that.
            Class clazz = String.class;

            if (field.getType() == int.class) {
                clazz = int.class;
            } else if (field.getType() == double.class) {
                clazz = double.class;
            } else if (field.getType() == boolean.class) {
                clazz = boolean.class;
            }

            Method m = tryToGetMethod(object, setter, clazz);
            //if you couldn't find more specific setter, back off
            //to string setter and try that.
            if (m == null && clazz != String.class) {
                m = tryToGetMethod(object, setter, String.class);
            }

            if (m != null) {
                String val = httpHeaders.getFirst(key);
                val = val.trim();
                if (clazz == String.class) {
                    checkTrustWorthy(setter, val);
                    m.invoke(object, val);
                } else if (clazz == int.class) {
                    m.invoke(object, Integer.parseInt(val));
                } else if (clazz == double.class) {
                    m.invoke(object, Double.parseDouble(val));
                } else if (clazz == boolean.class) {
                    m.invoke(object, Boolean.parseBoolean(val));
                } else {
                    throw new IllegalArgumentException("setter must be String, int, double or boolean...for now");
                }
            } else {
                throw new NoSuchMethodException("Couldn't find: "+setter);
            }

        } catch (Throwable ex) {
            throw new WebApplicationException(String.format(Locale.ROOT,
                    "%s is an invalid %s header", key, X_TIKA_OCR_HEADER_PREFIX));
        }
    }