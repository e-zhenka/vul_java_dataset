private static Node newNode() {
        Node build = NodeBuilder.nodeBuilder().local(true).data(true).settings(ImmutableSettings.builder()
                .put(ClusterName.SETTING, nodeName())
                .put("node.name", nodeName())
                .put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, 1)
                .put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, 0)
                .put(EsExecutors.PROCESSORS, 1) // limit the number of threads created
                .put("http.enabled", false)
                .put("index.store.type", "ram")
                .put("config.ignore_system_properties", true) // make sure we get what we set :)
                .put("gateway.type", "none")).build();
        build.start();
        assertThat(DiscoveryNode.localNode(build.settings()), is(true));
        return build;
    }