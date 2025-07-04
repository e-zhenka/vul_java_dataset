@Override
    protected Settings nodeSettings(int nodeOrdinal) {
        return ImmutableSettings.builder()
                .put(InternalNode.HTTP_ENABLED, true)
                .put(super.nodeSettings(nodeOrdinal)).build();
    }