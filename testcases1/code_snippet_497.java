protected User getUserByPattern(JNDIConnection connection, String username, String credentials, String[] attrIds,
            int curUserPattern) throws NamingException {

        User user = null;

        if (username == null || userPatternArray[curUserPattern] == null) {
            return null;
        }

        // Form the DistinguishedName from the user pattern.
        // Escape in case username contains a character with special meaning in
        // an attribute value.
        String dn = connection.userPatternFormatArray[curUserPattern].format(
                new String[] { doAttributeValueEscaping(username) });

        try {
            user = getUserByPattern(connection.context, username, attrIds, dn);
        } catch (NameNotFoundException e) {
            return null;
        } catch (NamingException e) {
            // If the getUserByPattern() call fails, try it again with the
            // credentials of the user that we're searching for
            try {
                userCredentialsAdd(connection.context, dn, credentials);

                user = getUserByPattern(connection.context, username, attrIds, dn);
            } finally {
                userCredentialsRemove(connection.context);
            }
        }
        return user;
    }