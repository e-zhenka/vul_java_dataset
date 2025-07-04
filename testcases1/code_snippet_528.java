@Override
    public Collection<FileAnnotation> parse(final InputStream file, final String moduleName) throws InvocationTargetException {
        try {
            SecureDigester digester = new SecureDigester(PmdParser.class);

            String rootXPath = "pmd";
            digester.addObjectCreate(rootXPath, Pmd.class);
            digester.addSetProperties(rootXPath);

            String fileXPath = "pmd/file";
            digester.addObjectCreate(fileXPath, hudson.plugins.pmd.parser.File.class);
            digester.addSetProperties(fileXPath);
            digester.addSetNext(fileXPath, "addFile", hudson.plugins.pmd.parser.File.class.getName());

            String bugXPath = "pmd/file/violation";
            digester.addObjectCreate(bugXPath, Violation.class);
            digester.addSetProperties(bugXPath);
            digester.addCallMethod(bugXPath, "setMessage", 0);
            digester.addSetNext(bugXPath, "addViolation", Violation.class.getName());

            Pmd module = (Pmd)digester.parse(file);
            if (module == null) {
                throw new SAXException("Input stream is not a PMD file.");
            }

            return convert(module, moduleName);
        }
        catch (IOException exception) {
            throw new InvocationTargetException(exception);
        }
        catch (SAXException exception) {
            throw new InvocationTargetException(exception);
        }
    }