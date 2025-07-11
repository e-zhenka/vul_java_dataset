private Ccm parseCCMXmlFile(InputStream ccmXmlFile) throws IOException, SAXException {
		Digester digester = new Digester();
		digester.setValidating(false);
		digester.setClassLoader(CcmParser.class.getClassLoader());

		String rootXPath = "ccm";
		digester.addObjectCreate(rootXPath, Ccm.class);
		digester.addSetProperties(rootXPath);

		String fileMetric = "ccm/metric";
		digester.addObjectCreate(fileMetric, Metric.class);
		digester.addSetProperties(fileMetric);
		digester.addBeanPropertySetter("ccm/metric/complexity");
		digester.addBeanPropertySetter("ccm/metric/unit");
		digester.addBeanPropertySetter("ccm/metric/classification");
		digester.addBeanPropertySetter("ccm/metric/file");
		digester.addBeanPropertySetter("ccm/metric/startLineNumber");
		digester.addBeanPropertySetter("ccm/metric/endLineNumber");
		digester.addSetNext(fileMetric, "addMetric", Metric.class.getName());

		return (Ccm)digester.parse(ccmXmlFile);
	}