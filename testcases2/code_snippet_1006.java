public void handleMessage(SoapMessage msg) throws Fault {
        if (msg.containsKey(SECURITY_PROCESSED) || isGET(msg)) {
            return;
        }
        //make sure we skip the URIMapping as we cannot apply security requirements to that
        msg.put(URIMappingInterceptor.URIMAPPING_SKIP, Boolean.TRUE);
        msg.put(SECURITY_PROCESSED, Boolean.TRUE);
        
        boolean utWithCallbacks = 
            MessageUtils.getContextualBoolean(msg, SecurityConstants.VALIDATE_TOKEN, true);
        translateProperties(msg);
        
        RequestData reqData = new CXFRequestData();

        WSSConfig config = (WSSConfig)msg.getContextualProperty(WSSConfig.class.getName()); 
        WSSecurityEngine engine;
        if (config != null) {
            engine = new WSSecurityEngine();
            engine.setWssConfig(config);
        } else {
            engine = getSecurityEngine(utWithCallbacks);
            if (engine == null) {
                engine = new WSSecurityEngine();
            }
            config = engine.getWssConfig();
        }
        reqData.setWssConfig(config);
        
                
        SOAPMessage doc = getSOAPMessage(msg);
        
        boolean doDebug = LOG.isLoggable(Level.FINE);
        boolean doTimeLog = TIME_LOG.isLoggable(Level.FINE);

        SoapVersion version = msg.getVersion();
        if (doDebug) {
            LOG.fine("WSS4JInInterceptor: enter handleMessage()");
        }

        long t0 = 0;
        long t1 = 0;
        long t2 = 0;
        long t3 = 0;

        if (doTimeLog) {
            t0 = System.currentTimeMillis();
        }

        /*
         * The overall try, just to have a finally at the end to perform some
         * housekeeping.
         */
        try {
            reqData.setMsgContext(msg);
            computeAction(msg, reqData);
            List<Integer> actions = new ArrayList<Integer>();
            String action = getAction(msg, version);

            int doAction = WSSecurityUtil.decodeAction(action, actions);

            String actor = (String)getOption(WSHandlerConstants.ACTOR);

            reqData.setCallbackHandler(getCallback(reqData, doAction, utWithCallbacks));
            
            String passwordTypeStrict = (String)getOption(WSHandlerConstants.PASSWORD_TYPE_STRICT);
            if (passwordTypeStrict == null) {
                setProperty(WSHandlerConstants.PASSWORD_TYPE_STRICT, "true");
            }
            
            // Configure replay caching
            ReplayCache nonceCache = 
                getReplayCache(
                    msg, SecurityConstants.ENABLE_NONCE_CACHE, SecurityConstants.NONCE_CACHE_INSTANCE
                );
            reqData.setNonceReplayCache(nonceCache);
            ReplayCache timestampCache = 
                getReplayCache(
                    msg, SecurityConstants.ENABLE_TIMESTAMP_CACHE, SecurityConstants.TIMESTAMP_CACHE_INSTANCE
                );
            reqData.setTimestampReplayCache(timestampCache);

            /*
             * Get and check the Signature specific parameters first because
             * they may be used for encryption too.
             */
            doReceiverAction(doAction, reqData);
            
            /*get chance to check msg context enableRevocation setting
             *when use policy based ws-security where the WSHandler configuration
             *isn't available
             */
            boolean enableRevocation = reqData.isRevocationEnabled() 
                || MessageUtils.isTrue(msg.getContextualProperty(SecurityConstants.ENABLE_REVOCATION));
            reqData.setEnableRevocation(enableRevocation);
            
            if (doTimeLog) {
                t1 = System.currentTimeMillis();
            }
            Element elem = WSSecurityUtil.getSecurityHeader(doc.getSOAPPart(), actor);

            List<WSSecurityEngineResult> wsResult = engine.processSecurityHeader(
                elem, reqData
            );

            if (doTimeLog) {
                t2 = System.currentTimeMillis();
            }

            if (wsResult != null && !wsResult.isEmpty()) { // security header found
                if (reqData.getWssConfig().isEnableSignatureConfirmation()) {
                    checkSignatureConfirmation(reqData, wsResult);
                }

                storeSignature(msg, reqData, wsResult);
                storeTimestamp(msg, reqData, wsResult);
                checkActions(msg, reqData, wsResult, actions);
                doResults(
                    msg, actor, 
                    SAAJUtils.getHeader(doc),
                    SAAJUtils.getBody(doc),
                    wsResult, utWithCallbacks
                );
            } else { // no security header found
                // Create an empty result list to pass into the required validation
                // methods.
                wsResult = new ArrayList<WSSecurityEngineResult>();
                if (doc.getSOAPPart().getEnvelope().getBody().hasFault()) {
                    LOG.warning("Request does not contain Security header, " 
                                + "but it's a fault.");
                    // We allow lax action matching here for backwards compatibility
                    // with manually configured WSS4JInInterceptors that previously
                    // allowed faults to pass through even if their actions aren't
                    // a strict match against those configured.  In the WS-SP case,
                    // we will want to still call doResults as it handles asserting
                    // certain assertions that do not require a WS-S header such as
                    // a sp:TransportBinding assertion.  In the case of WS-SP,
                    // the unasserted assertions will provide confirmation that
                    // security was not sufficient.
                    // checkActions(msg, reqData, wsResult, actions);
                    doResults(msg, actor, 
                              SAAJUtils.getHeader(doc),
                              SAAJUtils.getBody(doc),
                              wsResult);
                } else {
                    checkActions(msg, reqData, wsResult, actions);
                    doResults(msg, actor,
                              SAAJUtils.getHeader(doc),
                              SAAJUtils.getBody(doc),
                              wsResult);
                }
            }
            advanceBody(msg, SAAJUtils.getBody(doc));
            SAAJInInterceptor.replaceHeaders(doc, msg);

            if (doTimeLog) {
                t3 = System.currentTimeMillis();
                TIME_LOG.fine("Receive request: total= " + (t3 - t0) 
                        + " request preparation= " + (t1 - t0)
                        + " request processing= " + (t2 - t1) 
                        + " header, cert verify, timestamp= " + (t3 - t2) + "\n");
            }

            if (doDebug) {
                LOG.fine("WSS4JInInterceptor: exit handleMessage()");
            }

        } catch (WSSecurityException e) {
            LOG.log(Level.WARNING, "", e);
            SoapFault fault = createSoapFault(version, e);
            throw fault;
        } catch (XMLStreamException e) {
            throw new SoapFault(new Message("STAX_EX", LOG), e, version.getSender());
        } catch (SOAPException e) {
            throw new SoapFault(new Message("SAAJ_EX", LOG), e, version.getSender());
        } finally {
            reqData.clear();
            reqData = null;
        }
    }