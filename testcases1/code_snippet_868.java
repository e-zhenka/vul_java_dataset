public boolean matches(final byte[] message, final byte[] digest) {

        if (message == null) {
            return (digest == null);
        } else if (digest == null) {
            return false;
        }
        
        // Check initialization
        if (!isInitialized()) {
            initialize();
        }
            
        try {

            // If we are using a salt, extract it to use it.
            byte[] salt = null;
            if (this.useSalt) {
                // If we are using a salt generator which specifies the salt
                // to be included into the digest itself, get it from there.
                // If not, the salt is supposed to be fixed and thus the
                // salt generator can be safely asked for it again.
                if (this.saltGenerator.includePlainSaltInEncryptionResults()) {
                    
                    // Compute size figures and perform length checks
                    int digestSaltSize = this.saltSizeBytes;
                    if (this.digestLengthBytes > 0) {
                        if (this.useLenientSaltSizeCheck) {
                            if (digest.length < this.digestLengthBytes) {
                                throw new EncryptionOperationNotPossibleException();
                            }
                            digestSaltSize = digest.length - this.digestLengthBytes;
                        } else {
                            if (digest.length != (this.digestLengthBytes + this.saltSizeBytes)) {
                                throw new EncryptionOperationNotPossibleException();
                            }
                        }
                    } else {
                        // Salt size check behaviour cannot be set to lenient
                        if (digest.length < this.saltSizeBytes) {
                            throw new EncryptionOperationNotPossibleException();
                        }
                    }
                    
                    if (!this.invertPositionOfPlainSaltInEncryptionResults) {
                        salt = new byte[digestSaltSize];
                        System.arraycopy(digest, 0, salt, 0, digestSaltSize);
                    } else {
                        salt = new byte[digestSaltSize];
                        System.arraycopy(digest, digest.length - digestSaltSize, salt, 0, digestSaltSize);
                    }
                    
                } else {
                    salt = this.saltGenerator.generateSalt(this.saltSizeBytes);
                }
            }
            
            // Digest the message with the extracted digest.
            final byte[] encryptedMessage = digest(message, salt);
            
            // If, using the same salt, digests match, then messages too. 
            return (Arrays.equals(encryptedMessage, digest));
        
        } catch (Exception e) {
            // If digest fails, it is more secure not to return any information
            // about the cause in nested exceptions. Simply fail.
            throw new EncryptionOperationNotPossibleException();
        }
        
    }