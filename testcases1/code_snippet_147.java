@Override
    @SuppressWarnings("unchecked")
    public <T> T fromString(String content, Class<T> classOfT) {
        try (StringReader reader = new StringReader(content)) {
            JAXBContext jaxbContext = JAXBContext.newInstance(classOfT);

            XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
            xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, true);
            XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(reader);

            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            return (T) unmarshaller.unmarshal(xmlStreamReader);
        } catch (JAXBException | XMLStreamException e) {
            throw new PippoRuntimeException(e, "Failed to deserialize content to '{}'", classOfT.getName());
        }
    }