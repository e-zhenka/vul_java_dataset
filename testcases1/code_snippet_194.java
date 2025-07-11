public static DotCorpus parseDotCorpus(InputStream dotCorpusStream) throws CoreException {
    DocumentBuilderFactory documentBuilderFacoty = DocumentBuilderFactory.newInstance();

    DocumentBuilder documentBuilder;

    try {
      documentBuilder = documentBuilderFacoty.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      String message = "This should never happen:" + (e.getMessage() != null ? e.getMessage() : "");

      IStatus s = new Status(IStatus.ERROR, CasEditorPlugin.ID, IStatus.OK, message, e);

      throw new CoreException(s);
    }

    org.w3c.dom.Document dotCorpusDOM;

    try {
      dotCorpusDOM = documentBuilder.parse(dotCorpusStream);
    } catch (SAXException e) {
      String message = e.getMessage() != null ? e.getMessage() : "";

      IStatus s = new Status(IStatus.ERROR, CasEditorPlugin.ID, IStatus.OK, message, e);

      throw new CoreException(s);
    } catch (IOException e) {
      String message = e.getMessage() != null ? e.getMessage() : "";

      IStatus s = new Status(IStatus.ERROR, CasEditorPlugin.ID, IStatus.OK, message, e);

      throw new CoreException(s);
    }

    DotCorpus dotCorpus = new DotCorpus();

    // get corpora root element
    Element configElement = dotCorpusDOM.getDocumentElement();

    if (CONFIG_ELEMENT.equals(configElement.getNodeName())) {
      // TODO:
      // throw exception
    }

    NodeList corporaChildNodes = configElement.getChildNodes();

    for (int i = 0; i < corporaChildNodes.getLength(); i++) {
      Node corporaChildNode = corporaChildNodes.item(i);

      if (!(corporaChildNode instanceof Element)) {
        continue;
      }

      Element corporaChildElement = (Element) corporaChildNode;

      // TODO: This code will emit NumberFormatExceptions if the values
      // are incorrect, they should be caught, logged and replaced with default
      // values
      
      if (TYPESYSTEM_ELEMENT.equals(corporaChildElement.getNodeName())) {
        dotCorpus.setTypeSystemFilename(corporaChildElement.getAttribute(TYPESYTEM_FILE_ATTRIBUTE));
      } else if (CORPUS_ELEMENT.equals(corporaChildElement.getNodeName())) {
        String corpusFolderName = corporaChildElement.getAttribute(CORPUS_FOLDER_ATTRIBUTE);

        dotCorpus.addCorpusFolder(corpusFolderName);
      } else if (STYLE_ELEMENT.equals(corporaChildElement.getNodeName())) {
        String type = corporaChildElement.getAttribute(STYLE_TYPE_ATTRIBUTE);

        String styleString = corporaChildElement.getAttribute(STYLE_STYLE_ATTRIBUTE);

        int colorInteger = Integer
                .parseInt(corporaChildElement.getAttribute(STYLE_COLOR_ATTRIBUTE));

        Color color = new Color(colorInteger);

        String drawingLayerString = corporaChildElement.getAttribute(STYLE_LAYER_ATTRIBUTE);
        
        String drawingConfigString = corporaChildElement.getAttribute(STYLE_CONFIG_ATTRIBUTE);
        
        if (drawingConfigString.length() == 0)
          drawingConfigString = null;
        
        int drawingLayer;

        try {
          drawingLayer = Integer.parseInt(drawingLayerString);
        } catch (NumberFormatException e) {
          drawingLayer = 0;
        }

        AnnotationStyle style = new AnnotationStyle(type, AnnotationStyle.Style
                .valueOf(styleString), color, drawingLayer, drawingConfigString);

        dotCorpus.setStyle(style);
      } else if (CAS_PROCESSOR_ELEMENT.equals(corporaChildElement.getNodeName())) {
        dotCorpus.addCasProcessorFolder(corporaChildElement
                .getAttribute(CAS_PROCESSOR_FOLDER_ATTRIBUTE));
      } else if (EDITOR_ELEMENT.equals(corporaChildElement.getNodeName())) {
        String lineLengthHintString = corporaChildElement
                .getAttribute(EDITOR_LINE_LENGTH_ATTRIBUTE);

        int lineLengthHint = Integer.parseInt(lineLengthHintString);

        dotCorpus.setEditorLineLength(lineLengthHint);
      } else if (SHOWN_ELEMENT.equals(corporaChildElement.getNodeName())) {
        String type = corporaChildElement.getAttribute(SHOWN_TYPE_ATTRIBUTE);
        
        String isVisisbleString = corporaChildElement.getAttribute(SHOWN_IS_VISISBLE_ATTRIBUTE);
        
        boolean isVisible = Boolean.parseBoolean(isVisisbleString);
        
        if (isVisible) {
          dotCorpus.setShownType(type); 
        }
      }
      else {
        String message = "Unexpected element: " + corporaChildElement.getNodeName();

        IStatus s = new Status(IStatus.ERROR, CasEditorPlugin.ID, IStatus.OK, message, null);

        throw new CoreException(s);
      }
    }

    return dotCorpus;
  }