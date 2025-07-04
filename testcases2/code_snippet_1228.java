protected void applyAuthentication(LdapContext ctx, String userDn, String password) throws NamingException {
		ctx.addToEnvironment(Context.SECURITY_AUTHENTICATION, SIMPLE_AUTHENTICATION);
		ctx.addToEnvironment(Context.SECURITY_PRINCIPAL, userDn);
		ctx.addToEnvironment(Context.SECURITY_CREDENTIALS, password);
		// Force reconnect with user credentials
		ctx.reconnect(null);
	}