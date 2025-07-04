private static String parseSoapMethodName(InputStream stream, String charEncoding) {
		try {
			// newInstance() et pas newFactory() pour java 1.5 (issue 367)
			final XMLInputFactory factory = XMLInputFactory.newInstance();
			factory.setProperty(XMLInputFactory.SUPPORT_DTD, false); // disable DTDs entirely for that factory
			factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false); // disable external entities
			final XMLStreamReader xmlReader;
			if (charEncoding != null) {
				xmlReader = factory.createXMLStreamReader(stream, charEncoding);
			} else {
				xmlReader = factory.createXMLStreamReader(stream);
			}

			//best-effort parsing

			//start document, go to first tag
			xmlReader.nextTag();

			//expect first tag to be "Envelope"
			if (!"Envelope".equals(xmlReader.getLocalName())) {
				LOG.debug("Unexpected first tag of SOAP request: '" + xmlReader.getLocalName()
						+ "' (expected 'Envelope')");
				return null; //failed
			}

			//scan for body tag
			if (!scanForChildTag(xmlReader, "Body")) {
				LOG.debug("Unable to find SOAP 'Body' tag");
				return null; //failed
			}

			xmlReader.nextTag();

			//tag is method name
			return "." + xmlReader.getLocalName();
		} catch (final XMLStreamException e) {
			LOG.debug("Unable to parse SOAP request", e);
			//failed
			return null;
		}
	}