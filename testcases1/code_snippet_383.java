@Override
    protected void service(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        log.trace("Service: {}", request);

        // is there a consumer registered for the request.
        HttpConsumer consumer = getServletResolveConsumerStrategy().resolve(request, getConsumers());
        if (consumer == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (consumer.getEndpoint().getHttpMethodRestrict() != null) {
            Iterator it = ObjectHelper.createIterable(consumer.getEndpoint().getHttpMethodRestrict()).iterator();
            boolean match = false;
            while (it.hasNext()) {
                String method = it.next().toString();
                if (method.equalsIgnoreCase(request.getMethod())) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                return;
            }
        }

        if ("TRACE".equals(request.getMethod()) && !consumer.isTraceEnabled()) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        // we do not support java serialized objects unless explicit enabled
        String contentType = request.getContentType();
        if (HttpConstants.CONTENT_TYPE_JAVA_SERIALIZED_OBJECT.equals(contentType) && !consumer.getEndpoint().getComponent().isAllowJavaSerializedObject()) {
            System.out.println("415 miser !!!");
            response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
            return;
        }
        
        final Exchange result = (Exchange) request.getAttribute(EXCHANGE_ATTRIBUTE_NAME);
        if (result == null) {
            // no asynchronous result so leverage continuation
            final Continuation continuation = ContinuationSupport.getContinuation(request);
            if (continuation.isInitial() && continuationTimeout != null) {
                // set timeout on initial
                continuation.setTimeout(continuationTimeout);
            }

            // are we suspended and a request is dispatched initially?
            if (consumer.isSuspended() && continuation.isInitial()) {
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                return;
            }

            if (continuation.isExpired()) {
                String id = (String) continuation.getAttribute(EXCHANGE_ATTRIBUTE_ID);
                // remember this id as expired
                expiredExchanges.put(id, id);
                log.warn("Continuation expired of exchangeId: {}", id);
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                return;
            }

            // a new request so create an exchange
            final Exchange exchange = new DefaultExchange(consumer.getEndpoint(), ExchangePattern.InOut);

            if (consumer.getEndpoint().isBridgeEndpoint()) {
                exchange.setProperty(Exchange.SKIP_GZIP_ENCODING, Boolean.TRUE);
                exchange.setProperty(Exchange.SKIP_WWW_FORM_URLENCODED, Boolean.TRUE);
            }
            if (consumer.getEndpoint().isDisableStreamCache()) {
                exchange.setProperty(Exchange.DISABLE_HTTP_STREAM_CACHE, Boolean.TRUE);
            }
            
            HttpHelper.setCharsetFromContentType(request.getContentType(), exchange);
            
            exchange.setIn(new HttpMessage(exchange, request, response));
            // set context path as header
            String contextPath = consumer.getEndpoint().getPath();
            exchange.getIn().setHeader("CamelServletContextPath", contextPath);
            
            String httpPath = (String)exchange.getIn().getHeader(Exchange.HTTP_PATH);
            // here we just remove the CamelServletContextPath part from the HTTP_PATH
            if (contextPath != null
                && httpPath.startsWith(contextPath)) {
                exchange.getIn().setHeader(Exchange.HTTP_PATH,
                        httpPath.substring(contextPath.length()));
            }

            if (log.isTraceEnabled()) {
                log.trace("Suspending continuation of exchangeId: {}", exchange.getExchangeId());
            }
            continuation.setAttribute(EXCHANGE_ATTRIBUTE_ID, exchange.getExchangeId());

            // we want to handle the UoW
            try {
                consumer.createUoW(exchange);
            } catch (Exception e) {
                log.error("Error processing request", e);
                throw new ServletException(e);
            }

            // must suspend before we process the exchange
            continuation.suspend();

            ClassLoader oldTccl = overrideTccl(exchange);

            if (log.isTraceEnabled()) {
                log.trace("Processing request for exchangeId: {}", exchange.getExchangeId());
            }
            // use the asynchronous API to process the exchange
            
            consumer.getAsyncProcessor().process(exchange, new AsyncCallback() {
                public void done(boolean doneSync) {
                    // check if the exchange id is already expired
                    boolean expired = expiredExchanges.remove(exchange.getExchangeId()) != null;
                    if (!expired) {
                        if (log.isTraceEnabled()) {
                            log.trace("Resuming continuation of exchangeId: {}", exchange.getExchangeId());
                        }
                        // resume processing after both, sync and async callbacks
                        continuation.setAttribute(EXCHANGE_ATTRIBUTE_NAME, exchange);
                        continuation.resume();
                    } else {
                        log.warn("Cannot resume expired continuation of exchangeId: {}", exchange.getExchangeId());
                    }
                }
            });

            if (oldTccl != null) {
                restoreTccl(exchange, oldTccl);
            }
            
            // return to let Jetty continuation to work as it will resubmit and invoke the service
            // method again when its resumed
            return;
        }

        try {
            // now lets output to the response
            if (log.isTraceEnabled()) {
                log.trace("Resumed continuation and writing response for exchangeId: {}", result.getExchangeId());
            }
            Integer bs = consumer.getEndpoint().getResponseBufferSize();
            if (bs != null) {
                log.trace("Using response buffer size: {}", bs);
                response.setBufferSize(bs);
            }
            consumer.getBinding().writeResponse(result, response);
        } catch (IOException e) {
            log.error("Error processing request", e);
            throw e;
        } catch (Exception e) {
            log.error("Error processing request", e);
            throw new ServletException(e);
        } finally {
            consumer.doneUoW(result);
        }
    }