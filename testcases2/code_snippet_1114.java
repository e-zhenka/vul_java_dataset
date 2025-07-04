@Exported(name="property",inline=true)
    public List<UserProperty> getAllProperties() {
        if (hasPermission(Jenkins.ADMINISTER)) {
            return Collections.unmodifiableList(properties);
        }

        return Collections.emptyList();
    }