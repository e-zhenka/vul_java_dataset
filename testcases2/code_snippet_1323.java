private SCIMFilter scimFilter(String filter) throws SCIMException {
        SCIMFilter scimFilter;
        try {
            scimFilter = SCIMFilter.parse(filter);
        } catch (SCIMException e) {
            logger.debug("Attempting legacy scim filter conversion for [" + filter + "]", e);
            filter = filter.replaceAll("'","\"");
            scimFilter = SCIMFilter.parse(filter);
        }
        return scimFilter;
    }