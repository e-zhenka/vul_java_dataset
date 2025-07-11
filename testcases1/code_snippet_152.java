void nextRequest() {
        request.recycle();

        // Copy leftover bytes to the beginning of the buffer
        if (byteBuffer.remaining() > 0 && byteBuffer.position() > 0) {
            byteBuffer.compact();
            byteBuffer.flip();
        }
        // Always reset pos to zero
        byteBuffer.limit(byteBuffer.limit() - byteBuffer.position()).position(0);

        // Recycle filters
        for (int i = 0; i <= lastActiveFilter; i++) {
            activeFilters[i].recycle();
        }

        // Reset pointers
        lastActiveFilter = -1;
        parsingHeader = true;
        swallowInput = true;

        headerParsePos = HeaderParsePosition.HEADER_START;
        parsingRequestLine = true;
        parsingRequestLinePhase = 0;
        parsingRequestLineEol = false;
        parsingRequestLineStart = 0;
        parsingRequestLineQPos = -1;
        headerData.recycle();
    }