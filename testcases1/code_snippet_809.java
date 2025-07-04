@Override
  public void Authenticate(String user, String password) throws AuthenticationException {

    Hashtable<String, Object> env = new Hashtable<String, Object>();
    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
    env.put(Context.PROVIDER_URL, ldapURL);

    // If the domain is available in the config, then append it unless domain is
    // already part of the username. LDAP providers like Active Directory use a
    // fully qualified user name like foo@bar.com.
    if (!hasDomain(user) && ldapDomain != null) {
      user  = user + "@" + ldapDomain;
    }

    if (password == null || password.isEmpty()) {
      throw new AuthenticationException("Error validating LDAP user:" +
          " a null or blank password has been provided");
    }

    // setup the security principal
    String bindDN;
    if (baseDN == null) {
      bindDN = user;
    } else {
      bindDN = "uid=" + user + "," + baseDN;
    }
    env.put(Context.SECURITY_AUTHENTICATION, "simple");
    env.put(Context.SECURITY_PRINCIPAL, bindDN);
    env.put(Context.SECURITY_CREDENTIALS, password);

    try {
      // Create initial context
      Context ctx = new InitialDirContext(env);
      ctx.close();
    } catch (NamingException e) {
      throw new AuthenticationException("Error validating LDAP user", e);
    }
  }