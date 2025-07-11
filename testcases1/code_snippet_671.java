protected synchronized void decorateWithTLS(TLSClientParameters tlsClientParameters, 
            HttpURLConnection connection) throws GeneralSecurityException {

        
        int hash = tlsClientParameters.hashCode();
        if (hash != lastTlsHash) {
            lastTlsHash = hash;
            socketFactory = null;
        }
        
        // always reload socketFactory from HttpsURLConnection.defaultSSLSocketFactory and 
        // tlsClientParameters.sslSocketFactory to allow runtime configuration change
        if (tlsClientParameters.isUseHttpsURLConnectionDefaultSslSocketFactory()) {
            socketFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
            
        } else if (tlsClientParameters.getSSLSocketFactory() != null) {
            // see if an SSLSocketFactory was set. This allows easy interop
            // with not-yet-commons-ssl.jar, or even just people who like doing their
            // own JSSE.
            socketFactory = tlsClientParameters.getSSLSocketFactory();
            
        } else if (socketFactory == null) {
            // ssl socket factory not yet instantiated, create a new one with tlsClientParameters's Trust
            // Managers, Key Managers, etc

            SSLContext ctx = 
                org.apache.cxf.transport.https.SSLUtils.getSSLContext(tlsClientParameters);

            String[] cipherSuites = 
                SSLUtils.getCiphersuitesToInclude(tlsClientParameters.getCipherSuites(), 
                                                  tlsClientParameters.getCipherSuitesFilter(), 
                                                  ctx.getSocketFactory().getDefaultCipherSuites(),
                                                  SSLUtils.getSupportedCipherSuites(ctx), 
                                                  LOG);
            // The SSLSocketFactoryWrapper enables certain cipher suites
            // from the policy.
            String protocol = tlsClientParameters.getSecureSocketProtocol() != null ? tlsClientParameters
                .getSecureSocketProtocol() : "TLS";
            socketFactory = new SSLSocketFactoryWrapper(ctx.getSocketFactory(), cipherSuites,
                                                        protocol);
            //recalc the hashcode since some of the above MAY have changed the tlsClientParameters 
            lastTlsHash = tlsClientParameters.hashCode();
        } else {
           // ssl socket factory already initialized, reuse it to benefit of keep alive
        }
        
        
        HostnameVerifier verifier = org.apache.cxf.transport.https.SSLUtils
            .getHostnameVerifier(tlsClientParameters);
        
        if (connection instanceof HttpsURLConnection) {
            // handle the expected case (javax.net.ssl)
            HttpsURLConnection conn = (HttpsURLConnection) connection;
            conn.setHostnameVerifier(verifier);
            conn.setSSLSocketFactory(socketFactory);
        } else {
            // handle the deprecated sun case and other possible hidden API's 
            // that are similar to the Sun cases
            try {
                Method method = connection.getClass().getMethod("getHostnameVerifier");
                
                InvocationHandler handler = new ReflectionInvokationHandler(verifier) {
                    public Object invoke(Object proxy, 
                                         Method method, 
                                         Object[] args) throws Throwable {
                        try {
                            return super.invoke(proxy, method, args);
                        } catch (Exception ex) {
                            return false;
                        }
                    }
                };
                Object proxy = java.lang.reflect.Proxy.newProxyInstance(this.getClass().getClassLoader(),
                                                                        new Class[] {method.getReturnType()},
                                                                        handler);

                method = connection.getClass().getMethod("setHostnameVerifier", method.getReturnType());
                method.invoke(connection, proxy);
            } catch (Exception ex) {
                //Ignore this one
            }
            try {
                Method getSSLSocketFactory =  connection.getClass().getMethod("getSSLSocketFactory");
                Method setSSLSocketFactory = connection.getClass()
                    .getMethod("setSSLSocketFactory", getSSLSocketFactory.getReturnType());
                if (getSSLSocketFactory.getReturnType().isInstance(socketFactory)) {
                    setSSLSocketFactory.invoke(connection, socketFactory);
                } else {
                    //need to see if we can create one - mostly the weblogic case.   The 
                    //weblogic SSLSocketFactory has a protected constructor that can take
                    //a JSSE SSLSocketFactory so we'll try and use that
                    Constructor<?> c = getSSLSocketFactory.getReturnType()
                        .getDeclaredConstructor(SSLSocketFactory.class);
                    ReflectionUtil.setAccessible(c);
                    setSSLSocketFactory.invoke(connection, c.newInstance(socketFactory));
                }
            } catch (Exception ex) {
                if (connection.getClass().getName().contains("weblogic")) {
                    if (!weblogicWarned) {
                        weblogicWarned = true;
                        LOG.warning("Could not configure SSLSocketFactory on Weblogic.  "
                                    + " Use the Weblogic control panel to configure the SSL settings.");
                    }
                    return;
                } 
                //if we cannot set the SSLSocketFactor, we're in serious trouble.
                throw new IllegalArgumentException("Error decorating connection class " 
                        + connection.getClass().getName(), ex);
            }
        }
    }