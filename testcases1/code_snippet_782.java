@Override
  public void handle(RoutingContext context) {
    HttpServerRequest request = context.request();
    if (request.method() != HttpMethod.GET && request.method() != HttpMethod.HEAD) {
      if (log.isTraceEnabled()) log.trace("Not GET or HEAD so ignoring request");
      context.next();
    } else {
      // we are trying to match a URL path to a Filesystem path, so the first step
      // is to url decode the normalized path so avoid misinterpretations
      String path = Utils.urlDecode(context.normalisedPath(), false);

      if (path == null) {
        // if the normalized path is null it cannot be resolved
        log.warn("Invalid path: " + context.request().path());
        context.next();
        return;
      }

      if (File.separatorChar != '/') {
        // although forward slashes are not path separators according to the rfc3986 if
        // used directly to access the filesystem on Windows, they would be treated as such
        // Instead of relying on the usual normalized method, all forward slashes must be
        // replaced by backslashes in this handler.
        path = path.replace(File.separatorChar, '/');
      }
      // clean the .. sequences according to rfc3986
      path = Utils.removeDots(path);

      // only root is known for sure to be a directory. all other directories must be identified as such.
      if (!directoryListing && "/".equals(path)) {
        path = indexPage;
      }

      // can be called recursive for index pages
      sendStatic(context, path);

    }
  }