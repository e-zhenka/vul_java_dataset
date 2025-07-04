private void openConnection() {
        if (!connectionOpened) {
            connectionOpened = true;
            URLConnection connection = null;
            try {
                try {
                    connection = url.openConnection();
                } catch (IOException e) {
                    lastModified = null;
                    contentLength = null;
                    return;
                }
                if (url.getProtocol().equals("jar")) {
                    connection.setUseCaches(false);
                    URL jar = ((JarURLConnection) connection).getJarFileURL();
                    lastModified = new Date(new File(jar.getFile()).lastModified());
                } else {
                    lastModified = new Date(connection.getLastModified());
                }
                contentLength = connection.getContentLengthLong();
            } finally {
                if (connection != null) {
                    try {
                        IoUtils.safeClose(connection.getInputStream());
                    } catch (IOException e) {
                        //ignore
                    }
                }
            }
        }
    }