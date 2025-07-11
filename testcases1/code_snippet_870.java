public WireFormat createWireFormat() {
        WireFormatInfo info = new WireFormatInfo();
        info.setVersion(version);

        try {
            info.setStackTraceEnabled(stackTraceEnabled);
            info.setCacheEnabled(cacheEnabled);
            info.setTcpNoDelayEnabled(tcpNoDelayEnabled);
            info.setTightEncodingEnabled(tightEncodingEnabled);
            info.setSizePrefixDisabled(sizePrefixDisabled);
            info.setMaxInactivityDuration(maxInactivityDuration);
            info.setMaxInactivityDurationInitalDelay(maxInactivityDurationInitalDelay);
            info.setCacheSize(cacheSize);
            info.setMaxFrameSize(maxFrameSize);
            if( host!=null ) {
                info.setHost(host);
            }
            info.setProviderName(providerName);
            info.setProviderVersion(providerVersion);
            info.setPlatformDetails(platformDetails);
        } catch (Exception e) {
            IllegalStateException ise = new IllegalStateException("Could not configure WireFormatInfo");
            ise.initCause(e);
            throw ise;
        }

        OpenWireFormat f = new OpenWireFormat(version);
        f.setMaxFrameSize(maxFrameSize);
        f.setPreferedWireFormatInfo(info);
        return f;
    }