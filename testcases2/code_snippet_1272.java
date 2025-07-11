protected Object readResolve()
        throws ObjectStreamException {
        AbstractBrokerFactory factory = getPooledFactoryForKey(_poolKey);
        if (factory != null)
            return factory;

        // reset these transient fields to empty values
        _transactional = new ConcurrentHashMap<Object,Collection<Broker>>();
        _brokers = newBrokerSet();
        
        // turn off logging while de-serializing BrokerFactory
        String saveLogConfig = _conf.getLog();
        _conf.setLog("none");
        makeReadOnly();
        // re-enable any logging which was in effect
        _conf.setLog(saveLogConfig);  
        
        return this;
    }