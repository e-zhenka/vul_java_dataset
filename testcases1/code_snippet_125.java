@Override
    public String encodeForJSString(String source) {
        return source == null ? null : Encode.forJavaScript(source).replace("\\-", "\\u002D");
    }