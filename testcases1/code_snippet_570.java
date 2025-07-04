public static void setGlobalContext(Context newGlobalContext) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission(
                   ResourceLinkFactory.class.getName() + ".setGlobalContext"));
        }
        globalContext = newGlobalContext;
    }