public static String[] getCNs(final X509Certificate cert) {
        final LinkedList<String> cnList = new LinkedList<String>();
        /*
          Sebastian Hauer's original StrictSSLProtocolSocketFactory used
          getName() and had the following comment:

                Parses a X.500 distinguished name for the value of the
                "Common Name" field.  This is done a bit sloppy right
                 now and should probably be done a bit more according to
                <code>RFC 2253</code>.

           I've noticed that toString() seems to do a better job than
           getName() on these X500Principal objects, so I'm hoping that
           addresses Sebastian's concern.

           For example, getName() gives me this:
           1.2.840.113549.1.9.1=#16166a756c6975736461766965734063756362632e636f6d

           whereas toString() gives me this:
           EMAILADDRESS=juliusdavies@cucbc.com

           Looks like toString() even works with non-ascii domain names!
           I tested it with "&#x82b1;&#x5b50;.co.jp" and it worked fine.
        */

        final String subjectPrincipal = cert.getSubjectX500Principal().toString();
        final StringTokenizer st = new StringTokenizer(subjectPrincipal, ",+");
        while(st.hasMoreTokens()) {
            final String tok = st.nextToken().trim();
            if (tok.length() > 3) {
                if (tok.substring(0, 3).equalsIgnoreCase("CN=")) {
                    cnList.add(tok.substring(3));
                }
            }
        }
        if(!cnList.isEmpty()) {
            final String[] cns = new String[cnList.size()];
            cnList.toArray(cns);
            return cns;
        } else {
            return null;
        }
    }