@Override
    public void onCommand(final Object o) {
        final Command command = (Command)o;
        if (!closed.get() && command != null) {
            try {
                command.visit(new CommandVisitorAdapter() {
                    @Override
                    public Response processMessageDispatch(MessageDispatch md) throws Exception {
                        waitForTransportInterruptionProcessingToComplete();
                        ActiveMQDispatcher dispatcher = dispatchers.get(md.getConsumerId());
                        if (dispatcher != null) {
                            // Copy in case a embedded broker is dispatching via
                            // vm://
                            // md.getMessage() == null to signal end of queue
                            // browse.
                            Message msg = md.getMessage();
                            if (msg != null) {
                                msg = msg.copy();
                                msg.setReadOnlyBody(true);
                                msg.setReadOnlyProperties(true);
                                msg.setRedeliveryCounter(md.getRedeliveryCounter());
                                msg.setConnection(ActiveMQConnection.this);
                                msg.setMemoryUsage(null);
                                md.setMessage(msg);
                            }
                            dispatcher.dispatch(md);
                        } else {
                            LOG.debug("{} no dispatcher for {} in {}", this, md, dispatchers);
                        }
                        return null;
                    }

                    @Override
                    public Response processProducerAck(ProducerAck pa) throws Exception {
                        if (pa != null && pa.getProducerId() != null) {
                            ActiveMQMessageProducer producer = producers.get(pa.getProducerId());
                            if (producer != null) {
                                producer.onProducerAck(pa);
                            }
                        }
                        return null;
                    }

                    @Override
                    public Response processBrokerInfo(BrokerInfo info) throws Exception {
                        brokerInfo = info;
                        brokerInfoReceived.countDown();
                        optimizeAcknowledge &= !brokerInfo.isFaultTolerantConfiguration();
                        getBlobTransferPolicy().setBrokerUploadUrl(info.getBrokerUploadUrl());
                        return null;
                    }

                    @Override
                    public Response processConnectionError(final ConnectionError error) throws Exception {
                        executor.execute(new Runnable() {
                            @Override
                            public void run() {
                                onAsyncException(error.getException());
                            }
                        });
                        return null;
                    }

                    @Override
                    public Response processControlCommand(ControlCommand command) throws Exception {
                        return null;
                    }

                    @Override
                    public Response processConnectionControl(ConnectionControl control) throws Exception {
                        onConnectionControl((ConnectionControl)command);
                        return null;
                    }

                    @Override
                    public Response processConsumerControl(ConsumerControl control) throws Exception {
                        onConsumerControl((ConsumerControl)command);
                        return null;
                    }

                    @Override
                    public Response processWireFormat(WireFormatInfo info) throws Exception {
                        onWireFormatInfo((WireFormatInfo)command);
                        return null;
                    }
                });
            } catch (Exception e) {
                onClientInternalException(e);
            }
        }

        for (Iterator<TransportListener> iter = transportListeners.iterator(); iter.hasNext();) {
            TransportListener listener = iter.next();
            listener.onCommand(command);
        }
    }