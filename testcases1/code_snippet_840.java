protected Object readResolve()
        throws ObjectStreamException {
        AbstractBrokerFactory factory = getPooledFactory(_conf);
        if (factory != null)
            return factory;

        // reset these transient fields to empty values
        _transactional = new ConcurrentHashMap();
        _brokers = new ConcurrentReferenceHashSet(
                ConcurrentReferenceHashSet.WEAK);

        // turn off logging while de-serializing BrokerFactory
        String saveLogConfig = _conf.getLog();
        _conf.setLog("none");
        makeReadOnly();
        // re-enable any logging which was in effect
        _conf.setLog(saveLogConfig);
        
        return this;
    }