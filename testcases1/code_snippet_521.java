private static void writeString(ByteBuffer buffer, String string) {
        int length = string.length();
        for (int charIndex = 0; charIndex < length; charIndex++) {
            char c = string.charAt(charIndex);
            byte b = (byte) c;
            if(b != '\r' && b != '\n') {
                buffer.put(b);
            } else {
                buffer.put((byte) ' ');
            }
        }
    }