public String getPasswordValue(Object o) {
        if (o==null)    return null;
        if (o instanceof Secret)    return ((Secret)o).getEncryptedValue();
        if (getIsUnitTest()) {
            throw new SecurityException("attempted to render plaintext ‘" + o + "’ in password field; use a getter of type Secret instead");
        }
        return o.toString();
    }