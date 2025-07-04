@Override
    public HttpContent getContent(String pathInContext,int maxBufferSize)
        throws IOException
    {
        try
        {
            // try loading the content from our factory.
            Resource resource = _factory.getResource(pathInContext);
            HttpContent loaded = load(pathInContext, resource, maxBufferSize);
            return loaded;
        }
        catch (Throwable t)
        {
            // Any error has potential to reveal fully qualified path
            throw (InvalidPathException) new InvalidPathException(pathInContext, "Invalid PathInContext").initCause(t);
        }
    }