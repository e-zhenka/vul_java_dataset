protected void run() {
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setResourceClasses(MultipartStore.class);
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(AttachmentDeserializer.ATTACHMENT_MAX_SIZE, String.valueOf(1024 * 10));
        props.put(AttachmentDeserializer.ATTACHMENT_MEMORY_THRESHOLD, String.valueOf(1024 * 5));
        sf.setProperties(props);
        //default lifecycle is per-request, change it to singleton
        sf.setResourceProvider(MultipartStore.class,
                               new SingletonResourceProvider(new MultipartStore()));
        sf.setAddress("http://localhost:" + PORT + "/");

        server = sf.create();        
    }