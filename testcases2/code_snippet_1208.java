void addPathParam(String name, String value, boolean encoded) {
    if (relativeUrl == null) {
      // The relative URL is cleared when the first query parameter is set.
      throw new AssertionError();
    }
    String replacement = canonicalizeForPath(value, encoded);
    String newRelativeUrl = relativeUrl.replace("{" + name + "}", replacement);
    if (PATH_TRAVERSAL.matcher(newRelativeUrl).matches()) {
      throw new IllegalArgumentException(
          "@Path parameters shouldn't perform path traversal ('.' or '..'): " + value);
    }
    relativeUrl = newRelativeUrl;
  }