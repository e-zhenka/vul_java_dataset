@Override
    public boolean checkObjectExecutePermission(Class clazz, String methodName)
    {
        Boolean result = null;
        if (methodName != null) {
            for (Map.Entry<Class, Set<String>> classSetEntry : this.whitelistedMethods.entrySet()) {
                if (classSetEntry.getKey().isAssignableFrom(clazz)) {
                    result = classSetEntry.getValue().contains(methodName.toLowerCase());
                    break;
                }
            }
        }

        if (result == null) {
            result = super.checkObjectExecutePermission(clazz, methodName);
        }
        return result;
    }