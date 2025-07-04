private CoderResult decodeHasArray(ByteBuffer in, CharBuffer out) {
        int outRemaining = out.remaining();
        int pos = in.position();
        int limit = in.limit();
        final byte[] bArr = in.array();
        final char[] cArr = out.array();
        final int inIndexLimit = limit + in.arrayOffset();
        int inIndex = pos + in.arrayOffset();
        int outIndex = out.position() + out.arrayOffset();
        // if someone would change the limit in process,
        // he would face consequences
        for (; inIndex < inIndexLimit && outRemaining > 0; inIndex++) {
            int jchar = bArr[inIndex];
            if (jchar < 0) {
                jchar = jchar & 0x7F;
                // If first byte is invalid, tail will be set to -1
                int tail = remainingBytes[jchar];
                if (tail == -1) {
                    in.position(inIndex - in.arrayOffset());
                    out.position(outIndex - out.arrayOffset());
                    return CoderResult.malformedForLength(1);
                }
                // Additional checks to detect invalid sequences ASAP
                // Checks derived from Unicode 6.2, Chapter 3, Table 3-7
                // Check 2nd byte
                int tailAvailable = inIndexLimit - inIndex - 1;
                if (tailAvailable > 0) {
                    // First byte C2..DF, second byte 80..BF
                    if (jchar > 0x41 && jchar < 0x60 &&
                            (bArr[inIndex + 1] & 0xC0) != 0x80) {
                        in.position(inIndex - in.arrayOffset());
                        out.position(outIndex - out.arrayOffset());
                        return CoderResult.malformedForLength(1);
                    }
                    // First byte E0, second byte A0..BF
                    if (jchar == 0x60 && (bArr[inIndex + 1] & 0xE0) != 0xA0) {
                        in.position(inIndex - in.arrayOffset());
                        out.position(outIndex - out.arrayOffset());
                        return CoderResult.malformedForLength(1);
                    }
                    // First byte E1..EC, second byte 80..BF
                    if (jchar > 0x60 && jchar < 0x6D &&
                            (bArr[inIndex + 1] & 0xC0) != 0x80) {
                        in.position(inIndex - in.arrayOffset());
                        out.position(outIndex - out.arrayOffset());
                        return CoderResult.malformedForLength(1);
                    }
                    // First byte ED, second byte 80..9F
                    if (jchar == 0x6D && (bArr[inIndex + 1] & 0xE0) != 0x80) {
                        in.position(inIndex - in.arrayOffset());
                        out.position(outIndex - out.arrayOffset());
                        return CoderResult.malformedForLength(1);
                    }
                    // First byte EE..EF, second byte 80..BF
                    if (jchar > 0x6D && jchar < 0x70 &&
                            (bArr[inIndex + 1] & 0xC0) != 0x80) {
                        in.position(inIndex - in.arrayOffset());
                        out.position(outIndex - out.arrayOffset());
                        return CoderResult.malformedForLength(1);
                    }
                    // First byte F0, second byte 90..BF
                    if (jchar == 0x70 &&
                            ((bArr[inIndex + 1] & 0xFF) < 0x90 ||
                            (bArr[inIndex + 1] & 0xFF) > 0xBF)) {
                        in.position(inIndex - in.arrayOffset());
                        out.position(outIndex - out.arrayOffset());
                        return CoderResult.malformedForLength(1);
                    }
                    // First byte F1..F3, second byte 80..BF
                    if (jchar > 0x70 && jchar < 0x74 &&
                            (bArr[inIndex + 1] & 0xC0) != 0x80) {
                        in.position(inIndex - in.arrayOffset());
                        out.position(outIndex - out.arrayOffset());
                        return CoderResult.malformedForLength(1);
                    }
                    // First byte F4, second byte 80..8F
                    if (jchar == 0x74 &&
                            (bArr[inIndex + 1] & 0xF0) != 0x80) {
                        in.position(inIndex - in.arrayOffset());
                        out.position(outIndex - out.arrayOffset());
                        return CoderResult.malformedForLength(1);
                    }
                }
                // Check third byte if present and expected
                if (tailAvailable > 1 && tail > 1) {
                    if ((bArr[inIndex + 2] & 0xC0) != 0x80) {
                        in.position(inIndex - in.arrayOffset());
                        out.position(outIndex - out.arrayOffset());
                        return CoderResult.malformedForLength(2);
                    }
                }
                // Check fourth byte if present and expected
                if (tailAvailable > 2 && tail > 2) {
                    if ((bArr[inIndex + 3] & 0xC0) != 0x80) {
                        in.position(inIndex - in.arrayOffset());
                        out.position(outIndex - out.arrayOffset());
                        return CoderResult.malformedForLength(3);
                    }
                }
                if (tailAvailable < tail) {
                    break;
                }
                for (int i = 0; i < tail; i++) {
                    int nextByte = bArr[inIndex + i + 1] & 0xFF;
                    if ((nextByte & 0xC0) != 0x80) {
                        in.position(inIndex - in.arrayOffset());
                        out.position(outIndex - out.arrayOffset());
                        return CoderResult.malformedForLength(1 + i);
                    }
                    jchar = (jchar << 6) + nextByte;
                }
                jchar -= remainingNumbers[tail];
                if (jchar < lowerEncodingLimit[tail]) {
                    // Should have been encoded in fewer octets
                    in.position(inIndex - in.arrayOffset());
                    out.position(outIndex - out.arrayOffset());
                    return CoderResult.malformedForLength(1);
                }
                inIndex += tail;
            }
            // Apache Tomcat added test
            if (jchar >= 0xD800 && jchar <= 0xDFFF) {
                return CoderResult.unmappableForLength(3);
            }
            // Apache Tomcat added test
            if (jchar > 0x10FFFF) {
                return CoderResult.unmappableForLength(4);
            }
            if (jchar <= 0xffff) {
                cArr[outIndex++] = (char) jchar;
                outRemaining--;
            } else {
                if (outRemaining < 2) {
                    return CoderResult.OVERFLOW;
                }
                cArr[outIndex++] = (char) ((jchar >> 0xA) + 0xD7C0);
                cArr[outIndex++] = (char) ((jchar & 0x3FF) + 0xDC00);
                outRemaining -= 2;
            }
        }
        in.position(inIndex - in.arrayOffset());
        out.position(outIndex - out.arrayOffset());
        return (outRemaining == 0 && inIndex < inIndexLimit) ?
                CoderResult.OVERFLOW :
                CoderResult.UNDERFLOW;
    }