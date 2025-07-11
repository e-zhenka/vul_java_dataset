private State readHeaders(ByteBuf buffer) {
        final HttpMessage message = this.message;
        final HttpHeaders headers = message.headers();

        AppendableCharSequence line = headerParser.parse(buffer);
        if (line == null) {
            return null;
        }
        if (line.length() > 0) {
            do {
                char firstChar = line.charAtUnsafe(0);
                if (name != null && (firstChar == ' ' || firstChar == '\t')) {
                    //please do not make one line from below code
                    //as it breaks +XX:OptimizeStringConcat optimization
                    String trimmedLine = line.toString().trim();
                    String valueStr = String.valueOf(value);
                    value = valueStr + ' ' + trimmedLine;
                } else {
                    if (name != null) {
                        headers.add(name, value);
                    }
                    splitHeader(line);
                }

                line = headerParser.parse(buffer);
                if (line == null) {
                    return null;
                }
            } while (line.length() > 0);
        }

        // Add the last header.
        if (name != null) {
            headers.add(name, value);
        }

        // reset name and value fields
        name = null;
        value = null;

        List<String> values = headers.getAll(HttpHeaderNames.CONTENT_LENGTH);
        int contentLengthValuesCount = values.size();

        if (contentLengthValuesCount > 0) {
            // Guard against multiple Content-Length headers as stated in
            // https://tools.ietf.org/html/rfc7230#section-3.3.2:
            //
            // If a message is received that has multiple Content-Length header
            //   fields with field-values consisting of the same decimal value, or a
            //   single Content-Length header field with a field value containing a
            //   list of identical decimal values (e.g., "Content-Length: 42, 42"),
            //   indicating that duplicate Content-Length header fields have been
            //   generated or combined by an upstream message processor, then the
            //   recipient MUST either reject the message as invalid or replace the
            //   duplicated field-values with a single valid Content-Length field
            //   containing that decimal value prior to determining the message body
            //   length or forwarding the message.
            if (contentLengthValuesCount > 1 && message.protocolVersion() == HttpVersion.HTTP_1_1) {
                throw new IllegalArgumentException("Multiple Content-Length headers found");
            }
            contentLength = Long.parseLong(values.get(0));
        }

        if (isContentAlwaysEmpty(message)) {
            HttpUtil.setTransferEncodingChunked(message, false);
            return State.SKIP_CONTROL_CHARS;
        } else if (HttpUtil.isTransferEncodingChunked(message)) {
            // See https://tools.ietf.org/html/rfc7230#section-3.3.3
            //
            //       If a message is received with both a Transfer-Encoding and a
            //       Content-Length header field, the Transfer-Encoding overrides the
            //       Content-Length.  Such a message might indicate an attempt to
            //       perform request smuggling (Section 9.5) or response splitting
            //       (Section 9.4) and ought to be handled as an error.  A sender MUST
            //       remove the received Content-Length field prior to forwarding such
            //       a message downstream.
            //
            // This is also what http_parser does:
            // https://github.com/nodejs/http-parser/blob/v2.9.2/http_parser.c#L1769
            if (contentLengthValuesCount > 0 && message.protocolVersion() == HttpVersion.HTTP_1_1) {
                throw new IllegalArgumentException(
                        "Both 'Content-Length: " + contentLength + "' and 'Transfer-Encoding: chunked' found");
            }

            return State.READ_CHUNK_SIZE;
        } else if (contentLength() >= 0) {
            return State.READ_FIXED_LENGTH_CONTENT;
        } else {
            return State.READ_VARIABLE_LENGTH_CONTENT;
        }
    }