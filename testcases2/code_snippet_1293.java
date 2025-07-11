protected boolean parseChunkHeader()
        throws IOException {

        int result = 0;
        boolean eol = false;
        boolean readDigit = false;
        boolean extension = false;

        while (!eol) {

            if (pos >= lastValid) {
                if (readBytes() <= 0)
                    return false;
            }

            if (buf[pos] == Constants.CR || buf[pos] == Constants.LF) {
                parseCRLF(false);
                eol = true;
            } else if (buf[pos] == Constants.SEMI_COLON && !extension) {
                // First semi-colon marks the start of the extension. Further
                // semi-colons may appear to separate multiple chunk-extensions.
                // These need to be processed as part of parsing the extensions.
                extension = true;
                extensionSize++;
            } else if (!extension) {
                //don't read data after the trailer
                int charValue = HexUtils.getDec(buf[pos]);
                if (charValue != -1) {
                    readDigit = true;
                    result *= 16;
                    result += charValue;
                } else {
                    //we shouldn't allow invalid, non hex characters
                    //in the chunked header
                    return false;
                }
            } else {
                // Extension 'parsing'
                // Note that the chunk-extension is neither parsed nor
                // validated. Currently it is simply ignored.
                extensionSize++;
                if (maxExtensionSize > -1 && extensionSize > maxExtensionSize) {
                    throw new IOException("maxExtensionSize exceeded");
                }
            }

            // Parsing the CRLF increments pos
            if (!eol) {
                pos++;
            }

        }

        if (!readDigit)
            return false;

        if (result == 0)
            endChunk = true;

        remaining = result;
        if (remaining < 0)
            return false;

        return true;

    }