private static String localeToString(Locale locale) {
        if (locale != null) {
            return escapeXml(locale.toString());//locale.getDisplayName();
        } else {
            return "";
        }
    }