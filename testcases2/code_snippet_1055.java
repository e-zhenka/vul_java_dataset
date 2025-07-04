@Override
        public boolean check(Object credentials)
        {
            String password = (credentials instanceof String) ? (String) credentials : credentials.toString();

            try
            {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] ha1;
                if (credentials instanceof Credential.MD5)
                {
                    // Credentials are already a MD5 digest - assume it's in
                    // form user:realm:password (we have no way to know since
                    // it's a digest, alright?)
                    ha1 = ((Credential.MD5) credentials).getDigest();
                }
                else
                {
                    // calc A1 digest
                    md.update(username.getBytes(StandardCharsets.ISO_8859_1));
                    md.update((byte) ':');
                    md.update(realm.getBytes(StandardCharsets.ISO_8859_1));
                    md.update((byte) ':');
                    md.update(password.getBytes(StandardCharsets.ISO_8859_1));
                    ha1 = md.digest();
                }
                // calc A2 digest
                md.reset();
                md.update(method.getBytes(StandardCharsets.ISO_8859_1));
                md.update((byte) ':');
                md.update(uri.getBytes(StandardCharsets.ISO_8859_1));
                byte[] ha2 = md.digest();

                // calc digest
                // request-digest = <"> < KD ( H(A1), unq(nonce-value) ":"
                // nc-value ":" unq(cnonce-value) ":" unq(qop-value) ":" H(A2) )
                // <">
                // request-digest = <"> < KD ( H(A1), unq(nonce-value) ":" H(A2)
                // ) > <">

                md.update(TypeUtil.toString(ha1, 16).getBytes(StandardCharsets.ISO_8859_1));
                md.update((byte) ':');
                md.update(nonce.getBytes(StandardCharsets.ISO_8859_1));
                md.update((byte) ':');
                md.update(nc.getBytes(StandardCharsets.ISO_8859_1));
                md.update((byte) ':');
                md.update(cnonce.getBytes(StandardCharsets.ISO_8859_1));
                md.update((byte) ':');
                md.update(qop.getBytes(StandardCharsets.ISO_8859_1));
                md.update((byte) ':');
                md.update(TypeUtil.toString(ha2, 16).getBytes(StandardCharsets.ISO_8859_1));
                byte[] digest = md.digest();

                // check digest
                return stringEquals(TypeUtil.toString(digest, 16).toLowerCase(), response == null ? null : response.toLowerCase());
            }
            catch (Exception e)
            {
                LOG.warn(e);
            }

            return false;
        }