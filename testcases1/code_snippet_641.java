@Override
    public void receiveMessageHeader(final BasicContentHeaderProperties properties, final long bodySize)
    {
        if(LOGGER.isDebugEnabled())
        {
            LOGGER.debug("RECV[" + _channelId + "] MessageHeader[ properties: {" + properties + "} bodySize: " + bodySize + " ]");
        }

        if(hasCurrentMessage())
        {
            if(bodySize > _connection.getMaxMessageSize())
            {
                properties.dispose();
                closeChannel(ErrorCodes.MESSAGE_TOO_LARGE,
                             "Message size of " + bodySize + " greater than allowed maximum of " + _connection.getMaxMessageSize());
            }
            publishContentHeader(new ContentHeaderBody(properties, bodySize));
        }
        else
        {
            properties.dispose();
            _connection.sendConnectionClose(ErrorCodes.COMMAND_INVALID,
                                            "Attempt to send a content header without first sending a publish frame",
                                            _channelId);
        }
    }