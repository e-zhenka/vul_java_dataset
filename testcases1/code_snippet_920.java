public Map<String, Object> loadExport(Owner owner, File exportFile,
        ConflictOverrides overrides)
        throws ImporterException {
        File tmpDir = null;
        InputStream exportStream = null;
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            tmpDir = new SyncUtils(config).makeTempDir("import");
            extractArchive(tmpDir, exportFile);

            File signature = new File(tmpDir, "signature");
            if (signature.length() == 0) {
                throw new ImportExtractionException(i18n.tr("The archive does not " +
                                          "contain the required signature file"));
            }

            exportStream = new FileInputStream(new File(tmpDir, "consumer_export.zip"));
            boolean verifiedSignature = pki.verifySHA256WithRSAHashWithUpstreamCACert(
                exportStream,
                loadSignature(new File(tmpDir, "signature")));
            if (!verifiedSignature) {
                log.warn("Archive signature check failed.");
                if (!overrides
                    .isForced(Conflict.SIGNATURE_CONFLICT)) {

                    /*
                     * Normally for import conflicts that can be overridden, we try to
                     * report them all the first time so if the user intends to override,
                     * they can do so with just one more request. However in the case of
                     * a bad signature, we're going to report immediately due to the nature
                     * of what this might mean.
                     */
                    throw new ImportConflictException(
                        i18n.tr("Archive failed signature check"),
                        Conflict.SIGNATURE_CONFLICT);
                }
                else {
                    log.warn("Ignoring signature check failure.");
                }
            }

            File consumerExport = new File(tmpDir, "consumer_export.zip");
            File exportDir = extractArchive(tmpDir, consumerExport);

            Map<String, File> importFiles = new HashMap<String, File>();
            File[] listFiles = exportDir.listFiles();
            if (listFiles == null || listFiles.length == 0) {
                throw new ImportExtractionException(i18n.tr("The consumer_export " +
                    "archive has no contents"));
            }
            for (File file : listFiles) {
                importFiles.put(file.getName(), file);
            }

            ConsumerDto consumer = importObjects(owner, importFiles, overrides);
            Meta m = mapper.readValue(importFiles.get(ImportFile.META.fileName()),
                Meta.class);
            result.put("consumer", consumer);
            result.put("meta", m);
            return result;
        }
        catch (FileNotFoundException fnfe) {
            log.error("Archive file does not contain consumer_export.zip", fnfe);
            throw new ImportExtractionException(i18n.tr("The archive does not contain " +
                                           "the required consumer_export.zip file"));
        }
        catch (ConstraintViolationException cve) {
            log.error("Failed to import archive", cve);
            throw new ImporterException(i18n.tr("Failed to import archive"),
                cve);
        }
        catch (PersistenceException pe) {
            log.error("Failed to import archive", pe);
            throw new ImporterException(i18n.tr("Failed to import archive"),
                pe);
        }
        catch (IOException e) {
            log.error("Exception caught importing archive", e);
            throw new ImportExtractionException("unable to extract export archive", e);
        }
        catch (CertificateException e) {
            log.error("Certificate exception checking archive signature", e);
            throw new ImportExtractionException(
                "Certificate exception checking archive signature", e);
        }
        finally {
            if (tmpDir != null) {
                try {
                    FileUtils.deleteDirectory(tmpDir);
                }
                catch (IOException e) {
                    log.error("Failed to delete extracted export", e);
                }
            }
            if (exportStream != null) {
                try {
                    exportStream.close();
                }
                catch (Exception e) {
                    // nothing we can do.
                }
            }
        }
    }