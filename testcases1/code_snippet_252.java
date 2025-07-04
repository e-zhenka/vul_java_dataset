private static @Nullable User getOrCreate(@Nonnull String id, @Nonnull String fullName, boolean create) {
        String idkey = idStrategy().keyFor(id);

        byNameLock.readLock().lock();
        User u;
        try {
            u = AllUsers.byName().get(idkey);
        } finally {
            byNameLock.readLock().unlock();
        }
        final File configFile = getConfigFileFor(id);
        if (u == null && !configFile.isFile() && !configFile.getParentFile().isDirectory()) {
            // check for legacy users and migrate if safe to do so.
            File[] legacy = getLegacyConfigFilesFor(id);
            if (legacy != null && legacy.length > 0) {
                for (File legacyUserDir : legacy) {
                    final XmlFile legacyXml = new XmlFile(XSTREAM, new File(legacyUserDir, "config.xml"));
                    try {
                        Object o = legacyXml.read();
                        if (o instanceof User) {
                            if (idStrategy().equals(id, legacyUserDir.getName()) && !idStrategy().filenameOf(legacyUserDir.getName())
                                    .equals(legacyUserDir.getName())) {
                                if (!legacyUserDir.renameTo(configFile.getParentFile())) {
                                    LOGGER.log(Level.WARNING, "Failed to migrate user record from {0} to {1}",
                                            new Object[]{legacyUserDir, configFile.getParentFile()});
                                }
                                break;
                            }
                        } else {
                            LOGGER.log(Level.FINE, "Unexpected object loaded from {0}: {1}",
                                    new Object[]{ legacyUserDir, o });
                        }
                    } catch (IOException e) {
                        LOGGER.log(Level.FINE, String.format("Exception trying to load user from %s: %s",
                                new Object[]{ legacyUserDir, e.getMessage() }), e);
                    }
                }
            }
        }

        File unsanitizedLegacyConfigFile = getUnsanitizedLegacyConfigFileFor(id);
        boolean mustMigrateLegacyConfig = isMigrationRequiredForLegacyConfigFile(unsanitizedLegacyConfigFile, configFile);
        if (mustMigrateLegacyConfig) {
            File ancestor = unsanitizedLegacyConfigFile.getParentFile();
            if (!configFile.exists()) {
                try {
                    Files.createDirectory(configFile.getParentFile().toPath());
                    Files.move(unsanitizedLegacyConfigFile.toPath(), configFile.toPath());
                } catch (IOException | InvalidPathException e) {
                    LOGGER.log(
                            Level.WARNING,
                            String.format("Failed to migrate user record from %s to %s, see SECURITY-499 for more information", idStrategy().legacyFilenameOf(id), idStrategy().filenameOf(id)),
                            e);
                }
            }

            // Don't clean up ancestors with other children; the directories should be cleaned up when the last child
            // is migrated
            File tmp = ancestor;
            try {
                while (!ancestor.equals(getRootDir())) {
                    try (DirectoryStream<Path> stream = Files.newDirectoryStream(ancestor.toPath())) {
                        if (!stream.iterator().hasNext()) {
                            tmp = ancestor;
                            ancestor = tmp.getParentFile();
                            Files.deleteIfExists(tmp.toPath());
                        } else {
                            break;
                        }
                    }
                }
            } catch (IOException | InvalidPathException e) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Could not delete " + tmp + " when cleaning up legacy user directories", e);
                }
            }
        }

        if (u==null && (create || configFile.exists())) {
            User tmp = new User(id, fullName);
            User prev;
            byNameLock.readLock().lock();
            try {
                prev = AllUsers.byName().putIfAbsent(idkey, u = tmp);
            } finally {
                byNameLock.readLock().unlock();
            }
            if (prev != null) {
                u = prev; // if some has already put a value in the map, use it
                if (LOGGER.isLoggable(Level.FINE) && !fullName.equals(prev.getFullName())) {
                    LOGGER.log(Level.FINE, "mismatch on fullName (‘" + fullName + "’ vs. ‘" + prev.getFullName() + "’) for ‘" + id + "’", new Throwable());
                }
            } else if (!id.equals(fullName) && !configFile.exists()) {
                // JENKINS-16332: since the fullName may not be recoverable from the id, and various code may store the id only, we must save the fullName
                try {
                    u.save();
                } catch (IOException x) {
                    LOGGER.log(Level.WARNING, null, x);
                }
            }
        }
        return u;
    }