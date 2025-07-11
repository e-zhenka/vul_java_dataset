protected final File file(String name, boolean mustExist) {

        if (name.equals("/")) {
            name = "";
        }
        File file = new File(fileBase, name);

        // If the requested names ends in '/', the Java File API will return a
        // matching file if one exists. This isn't what we want as it is not
        // consistent with the Servlet spec rules for request mapping.
        if (name.endsWith("/") && file.isFile()) {
            return null;
        }

        // If the file/dir must exist but the identified file/dir can't be read
        // then signal that the resource was not found
        if (mustExist && !file.canRead()) {
            return null;
        }

        // If allow linking is enabled, files are not limited to being located
        // under the fileBase so all further checks are disabled.
        if (getRoot().getAllowLinking()) {
            return file;
        }

        // Check that this file is located under the WebResourceSet's base
        String canPath = null;
        try {
            canPath = file.getCanonicalPath();
        } catch (IOException e) {
            // Ignore
        }
        if (canPath == null || !canPath.startsWith(canonicalBase)) {
            return null;
        }

        // Ensure that the file is not outside the fileBase. This should not be
        // possible for standard requests (the request is normalized early in
        // the request processing) but might be possible for some access via the
        // Servlet API (RequestDispatcher, HTTP/2 push etc.) therefore these
        // checks are retained as an additional safety measure
        // absoluteBase has been normalized so absPath needs to be normalized as
        // well.
        String absPath = normalize(file.getAbsolutePath());
        if (absoluteBase.length() > absPath.length()) {
            return null;
        }

        // Remove the fileBase location from the start of the paths since that
        // was not part of the requested path and the remaining check only
        // applies to the request path
        absPath = absPath.substring(absoluteBase.length());
        canPath = canPath.substring(canonicalBase.length());

        // Case sensitivity check
        // The normalized requested path should be an exact match the equivalent
        // canonical path. If it is not, possible reasons include:
        // - case differences on case insensitive file systems
        // - Windows removing a trailing ' ' or '.' from the file name
        //
        // In all cases, a mis-match here results in the resource not being
        // found
        //
        // absPath is normalized so canPath needs to be normalized as well
        // Can't normalize canPath earlier as canonicalBase is not normalized
        if (canPath.length() > 0) {
            canPath = normalize(canPath);
        }
        if (!canPath.equals(absPath)) {
            return null;
        }

        return file;
    }