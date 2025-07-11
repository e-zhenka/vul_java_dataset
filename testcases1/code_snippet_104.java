private Schema loadSchema(String schemaResource) {
		ClassLoader loader = run( GetClassLoader.fromClass( XmlParserHelper.class ) );

		URL schemaUrl = run( GetResource.action( loader, schemaResource ) );
		SchemaFactory sf = SchemaFactory.newInstance( javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI );
		Schema schema = null;
		try {
			schema = sf.newSchema( schemaUrl );
		}
		catch ( SAXException e ) {
			log.unableToCreateSchema( schemaResource, e.getMessage() );
		}
		return schema;
	}