static Secret tryDecrypt(Cipher cipher, byte[] in) throws UnsupportedEncodingException {
        try {
            String plainText = new String(cipher.doFinal(in), "UTF-8");
            if(plainText.endsWith(MAGIC))
                return new Secret(plainText.substring(0,plainText.length()-MAGIC.length()));
            return null;
        } catch (GeneralSecurityException e) {
            return null; // if the key doesn't match with the bytes, it can result in BadPaddingException
        }
    }