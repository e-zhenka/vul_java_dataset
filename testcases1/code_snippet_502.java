@SuppressWarnings("unchecked")
    public This transformObject(Saml2ObjectTransformer<T> tr) {
        final StringTransformer original = this.transformer;
        this.transformer = s -> {
            final String originalTransformed = original.transform(s);

            if (originalTransformed == null) {
                return null;
            }

            final ByteArrayInputStream baos = new ByteArrayInputStream(originalTransformed.getBytes());
            final T saml2Object = (T) new SAML2Response().getSAML2ObjectFromStream(baos);
            final T transformed = tr.transform(saml2Object);

            if (transformed == null) {
                return null;
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            XMLStreamWriter xmlStreamWriter = StaxUtil.getXMLStreamWriter(bos);

            if (transformed instanceof AuthnRequestType) {
                new SAMLRequestWriter(xmlStreamWriter).write((AuthnRequestType) transformed);
            } else if (transformed instanceof LogoutRequestType) {
                new SAMLRequestWriter(xmlStreamWriter).write((LogoutRequestType) transformed);
            } else if (transformed instanceof ArtifactResolveType) {
                new SAMLRequestWriter(xmlStreamWriter).write((ArtifactResolveType) transformed);
            } else if (transformed instanceof AttributeQueryType) {
                new SAMLRequestWriter(xmlStreamWriter).write((AttributeQueryType) transformed);
            } else if (transformed instanceof ResponseType) {
                new SAMLResponseWriter(xmlStreamWriter).write((ResponseType) transformed);
            } else if (transformed instanceof ArtifactResponseType) {
                new SAMLResponseWriter(xmlStreamWriter).write((ArtifactResponseType) transformed);
            } else if (transformed instanceof StatusResponseType) {
                new SAMLResponseWriter(xmlStreamWriter).write((StatusResponseType) transformed, SAMLProtocolQNames.LOGOUT_RESPONSE.getQName("samlp"));
            } else {
                Assert.assertNotNull("Unknown type: <null>", transformed);
                Assert.fail("Unknown type: " + transformed.getClass().getName());
            }
            return new String(bos.toByteArray(), GeneralConstants.SAML_CHARSET);
        };
        return (This) this;
    }