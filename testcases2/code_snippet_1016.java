protected void createWebXmlDigester(boolean namespaceAware,
            boolean validation) {
        
        if (!namespaceAware && !validation) {
            webDigester = webDigesters[0];
            webFragmentDigester = webFragmentDigesters[0];
            
        } else if (!namespaceAware && validation) {
            webDigester = webDigesters[1];
            webFragmentDigester = webFragmentDigesters[1];
            
        } else if (namespaceAware && !validation) {
            webDigester = webDigesters[2];
            webFragmentDigester = webFragmentDigesters[2];
            
        } else {
            webDigester = webDigesters[3];
            webFragmentDigester = webFragmentDigesters[3];
        }
    }