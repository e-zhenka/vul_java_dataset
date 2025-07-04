public static String[] getCNs(final X509Certificate cert) {
        final String subjectPrincipal = cert.getSubjectX500Principal().toString();
        try {
            return extractCNs(subjectPrincipal);
        } catch (SSLException ex) {
            return null;
        }
    }