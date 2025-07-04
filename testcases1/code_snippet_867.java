@Override
    public boolean check(String uri, Resource resource)
    {
        // Only support PathResource alias checking
        if (!(resource instanceof PathResource))
            return false;
        
        PathResource pathResource = (PathResource)resource;

        try
        {
            Path path = pathResource.getPath();
            Path alias = pathResource.getAliasPath();
            
            // is the file itself a symlink?
            if (Files.isSymbolicLink(path))
            {        
                alias = path.getParent().resolve(alias);
                if (LOG.isDebugEnabled())
                {
                    LOG.debug("path ={}",path);
                    LOG.debug("alias={}",alias);
                }
                if (Files.isSameFile(path,alias))
                {
                    if (LOG.isDebugEnabled())
                        LOG.debug("Allow symlink {} --> {}",resource,pathResource.getAliasPath());
                    return true;
                }
            }

            // No, so let's check each element ourselves
            boolean linked=true;
            Path target=path;
            int loops=0;
            while (linked)
            {
                if (++loops>100)
                {
                    if (LOG.isDebugEnabled())
                        LOG.debug("Too many symlinks {} --> {}",resource,target);
                    return false;
                }
                linked=false;
                Path d = target.getRoot();
                for (Path e:target)
                {
                    Path r=d.resolve(e);
                    d=r;

                    while (Files.exists(d) && Files.isSymbolicLink(d))
                    {
                        Path link=Files.readSymbolicLink(d);    
                        if (!link.isAbsolute())
                            link=d.getParent().resolve(link);
                        d=link;
                        linked=true;
                    }
                }
                target=d;
            }
            
            if (pathResource.getAliasPath().equals(target))
            {
                if (LOG.isDebugEnabled())
                    LOG.debug("Allow path symlink {} --> {}",resource,target);
                return true;
            }
        }
        catch(Exception e)
        {
            LOG.ignore(e);
        }
        
        return false;
    }