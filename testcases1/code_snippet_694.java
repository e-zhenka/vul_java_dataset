public QueryTargetPolicy getQueryTargetPolicyInstance() {
        if (queryTargetPolicyPlugin.get() == null) {
            queryTargetPolicyPlugin.instantiate(QueryTargetPolicy.class,
                    this, true);
        }
        return (QueryTargetPolicy) queryTargetPolicyPlugin.get();
    }