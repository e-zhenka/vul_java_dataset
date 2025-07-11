public String encodeCharacter( char[] immune, Character c )
	{
		String cStr = String.valueOf(c.charValue());
		byte[] bytes;
		StringBuilder sb;

        // check for user specified immune characters
        if ( immune != null && containsCharacter( c.charValue(), immune ) )
            return cStr;

        // check for standard characters (e.g., alphanumeric, etc.)
		if(UNENCODED_SET.contains(c))
			return cStr;

		bytes = toUtf8Bytes(cStr);
		sb = new StringBuilder(bytes.length * 3);
		for(byte b : bytes)
			appendTwoUpperHex(sb.append('%'), b);
		return sb.toString();
	}