private static String verifyRedirectUri(UriInfo uriInfo, String rootUrl, String redirectUri, RealmModel realm, Set<String> validRedirects, boolean requireRedirectUri) {

        if (redirectUri != null)
            redirectUri = normalizeUrl(redirectUri);

        if (redirectUri == null) {
            if (!requireRedirectUri) {
                redirectUri = getSingleValidRedirectUri(validRedirects);
            }

            if (redirectUri == null) {
                logger.debug("No Redirect URI parameter specified");
                return null;
            }
        } else if (validRedirects.isEmpty()) {
            logger.debug("No Redirect URIs supplied");
            redirectUri = null;
        } else {
            redirectUri = lowerCaseHostname(redirectUri);

            String r = redirectUri;
            Set<String> resolveValidRedirects = resolveValidRedirects(uriInfo, rootUrl, validRedirects);

            boolean valid = matchesRedirects(resolveValidRedirects, r);

            if (!valid && r.startsWith(Constants.INSTALLED_APP_URL) && r.indexOf(':', Constants.INSTALLED_APP_URL.length()) >= 0) {
                int i = r.indexOf(':', Constants.INSTALLED_APP_URL.length());

                StringBuilder sb = new StringBuilder();
                sb.append(r.substring(0, i));

                i = r.indexOf('/', i);
                if (i >= 0) {
                    sb.append(r.substring(i));
                }

                r = sb.toString();

                valid = matchesRedirects(resolveValidRedirects, r);
            }
            if (valid && redirectUri.startsWith("/")) {
                redirectUri = relativeToAbsoluteURI(uriInfo, rootUrl, redirectUri);
            }
            redirectUri = valid ? redirectUri : null;
        }

        if (Constants.INSTALLED_APP_URN.equals(redirectUri)) {
            return Urls.realmInstalledAppUrnCallback(uriInfo.getBaseUri(), realm.getName()).toString();
        } else {
            return redirectUri;
        }
    }