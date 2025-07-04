@Override
    public Collection<FileAnnotation> parse(final InputStream file, final String moduleName)
            throws InvocationTargetException {
        try {
            SecureDigester digester = new SecureDigester(LintParser.class);

            List<LintIssue> issues = new ArrayList<LintIssue>();
            digester.push(issues);

            String issueXPath = "issues/issue";
            digester.addObjectCreate(issueXPath, LintIssue.class);
            digester.addSetProperties(issueXPath);
            digester.addSetNext(issueXPath, "add");

            String locationXPath = issueXPath + "/location";
            digester.addObjectCreate(locationXPath, Location.class);
            digester.addSetProperties(locationXPath);
            digester.addSetNext(locationXPath, "addLocation", Location.class.getName());

            digester.parse(file);

            return convert(issues, moduleName);
        } catch (IOException | SAXException exception) {
            throw new InvocationTargetException(exception);
        }
    }