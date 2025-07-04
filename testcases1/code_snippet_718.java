private javax.crypto.Cipher initNewCipher(int jcaCipherMode, byte[] key, byte[] iv, boolean streaming)
            throws CryptoException {

        javax.crypto.Cipher cipher = newCipherInstance(streaming);
        java.security.Key jdkKey = new SecretKeySpec(key, getAlgorithmName());
        AlgorithmParameterSpec ivSpec = null;

        if (iv != null && iv.length > 0) {
            ivSpec = createParameterSpec(iv, streaming);
        }

        init(cipher, jcaCipherMode, jdkKey, ivSpec, getSecureRandom());

        return cipher;
    }