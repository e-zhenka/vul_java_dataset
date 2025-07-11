@Override
    public final void emitHeader(String name, String value) throws HpackException {
        if (log.isDebugEnabled()) {
            log.debug(sm.getString("stream.header.debug", getConnectionId(), getIdentifier(),
                    name, value));
        }

        // Header names must be lower case
        if (!name.toLowerCase(Locale.US).equals(name)) {
            throw new HpackException(sm.getString("stream.header.case",
                    getConnectionId(), getIdentifier(), name));
        }

        if ("connection".equals(name)) {
            throw new HpackException(sm.getString("stream.header.connection",
                    getConnectionId(), getIdentifier()));
        }

        if ("te".equals(name)) {
            if (!"trailers".equals(value)) {
                throw new HpackException(sm.getString("stream.header.te",
                        getConnectionId(), getIdentifier(), value));
            }
        }

        if (headerStateErrorMsg != null) {
            // Don't bother processing the header since the stream is going to
            // be reset anyway
            return;
        }

        boolean pseudoHeader = name.charAt(0) == ':';

        if (pseudoHeader && headerState != HEADER_STATE_PSEUDO) {
            headerStateErrorMsg = sm.getString("stream.header.unexpectedPseudoHeader",
                    getConnectionId(), getIdentifier(), name);
            // No need for further processing. The stream will be reset.
            return;
        }

        if (headerState == HEADER_STATE_PSEUDO && !pseudoHeader) {
            headerState = HEADER_STATE_REGULAR;
        }

        switch(name) {
        case ":method": {
            if (coyoteRequest.method().isNull()) {
                coyoteRequest.method().setString(value);
            } else {
                throw new HpackException(sm.getString("stream.header.duplicate",
                        getConnectionId(), getIdentifier(), ":method" ));
            }
            break;
        }
        case ":scheme": {
            if (coyoteRequest.scheme().isNull()) {
                coyoteRequest.scheme().setString(value);
            } else {
                throw new HpackException(sm.getString("stream.header.duplicate",
                        getConnectionId(), getIdentifier(), ":scheme" ));
            }
            break;
        }
        case ":path": {
            if (!coyoteRequest.requestURI().isNull()) {
                throw new HpackException(sm.getString("stream.header.duplicate",
                        getConnectionId(), getIdentifier(), ":path" ));
            }
            if (value.length() == 0) {
                throw new HpackException(sm.getString("stream.header.noPath",
                        getConnectionId(), getIdentifier()));
            }
            int queryStart = value.indexOf('?');
            String uri;
            if (queryStart == -1) {
                uri = value;
            } else {
                uri = value.substring(0, queryStart);
                String query = value.substring(queryStart + 1);
                coyoteRequest.queryString().setString(query);
            }
            // Bug 61120. Set the URI as bytes rather than String so any path
            // parameters are correctly processed
            byte[] uriBytes = uri.getBytes(StandardCharsets.ISO_8859_1);
            coyoteRequest.requestURI().setBytes(uriBytes, 0, uriBytes.length);
            break;
        }
        case ":authority": {
            if (coyoteRequest.serverName().isNull()) {
                int i = value.lastIndexOf(':');
                if (i > -1) {
                    coyoteRequest.serverName().setString(value.substring(0, i));
                    coyoteRequest.setServerPort(Integer.parseInt(value.substring(i + 1)));
                } else {
                    coyoteRequest.serverName().setString(value);
                }
            } else {
                throw new HpackException(sm.getString("stream.header.duplicate",
                        getConnectionId(), getIdentifier(), ":authority" ));
            }
            break;
        }
        case "cookie": {
            // Cookie headers need to be concatenated into a single header
            // See RFC 7540 8.1.2.5
            if (cookieHeader == null) {
                cookieHeader = new StringBuilder();
            } else {
                cookieHeader.append("; ");
            }
            cookieHeader.append(value);
            break;
        }
        default: {
            if (headerState == HEADER_STATE_TRAILER && !handler.isTrailerHeaderAllowed(name)) {
                break;
            }
            if ("expect".equals(name) && "100-continue".equals(value)) {
                coyoteRequest.setExpectation(true);
            }
            if (pseudoHeader) {
                headerStateErrorMsg = sm.getString("stream.header.unknownPseudoHeader",
                        getConnectionId(), getIdentifier(), name);
            }

            if (headerState == HEADER_STATE_TRAILER) {
                // HTTP/2 headers are already always lower case
                coyoteRequest.getTrailerFields().put(name, value);
            } else {
                coyoteRequest.getMimeHeaders().addValue(name).setString(value);
            }
        }
        }
    }