@Override
	protected Object doZipTransform(final Message<?> message) throws Exception {

		try {
			final Object payload = message.getPayload();
			final Object unzippedData;

			InputStream inputStream = null;

			try {
				if (payload instanceof File) {
					final File filePayload = (File) payload;

					if (filePayload.isDirectory()) {
						throw new UnsupportedOperationException(String.format("Cannot unzip a directory: '%s'",
								filePayload.getAbsolutePath()));
					}

					if (!SpringZipUtils.isValid(filePayload)) {
						throw new IllegalStateException(String.format("Not a zip file: '%s'.",
								filePayload.getAbsolutePath()));
					}

					inputStream = new FileInputStream(filePayload);
				}
				else if (payload instanceof InputStream) {
					inputStream = (InputStream) payload;
				}
				else if (payload instanceof byte[]) {
					inputStream = new ByteArrayInputStream((byte[]) payload);
				}
				else {
					throw new IllegalArgumentException(String.format("Unsupported payload type '%s'. " +
									"The only supported payload types are java.io.File, byte[] and java.io.InputStream",
							payload.getClass().getSimpleName()));
				}

				final SortedMap<String, Object> uncompressedData = new TreeMap<String, Object>();

				ZipUtil.iterate(inputStream, new ZipEntryCallback() {

					@Override
					public void process(InputStream zipEntryInputStream, ZipEntry zipEntry) throws IOException {

						final String zipEntryName = zipEntry.getName();
						final long zipEntryTime = zipEntry.getTime();
						final long zipEntryCompressedSize = zipEntry.getCompressedSize();
						final String type = zipEntry.isDirectory() ? "directory" : "file";

						if (logger.isInfoEnabled()) {
							logger.info(String.format("Unpacking Zip Entry - Name: '%s',Time: '%s', " +
											"Compressed Size: '%s', Type: '%s'",
									zipEntryName, zipEntryTime, zipEntryCompressedSize, type));
						}

						if (ZipResultType.FILE.equals(zipResultType)) {
							final File destinationFile = checkPath(message, zipEntryName);

							if (zipEntry.isDirectory()) {
								destinationFile.mkdirs(); //NOSONAR false positive
							}
							else {
								SpringZipUtils.copy(zipEntryInputStream, destinationFile);
								uncompressedData.put(zipEntryName, destinationFile);
							}
						}
						else if (ZipResultType.BYTE_ARRAY.equals(zipResultType)) {
							if (!zipEntry.isDirectory()) {
								checkPath(message, zipEntryName);
								byte[] data = IOUtils.toByteArray(zipEntryInputStream);
								uncompressedData.put(zipEntryName, data);
							}
						}
						else {
							throw new IllegalStateException("Unsupported zipResultType " + zipResultType);
						}
					}

					public File checkPath(final Message<?> message, final String zipEntryName) throws IOException {
						final File tempDir = new File(workDirectory, message.getHeaders().getId().toString());
						tempDir.mkdirs(); //NOSONAR false positive
						final File destinationFile = new File(tempDir, zipEntryName);

						/* If we see the relative traversal string of ".." we need to make sure
						 * that the outputdir + name doesn't leave the outputdir.
						 */
						if (!destinationFile.getCanonicalPath().startsWith(workDirectory.getCanonicalPath())) {
							throw new ZipException("The file " + zipEntryName +
									" is trying to leave the target output directory of " + workDirectory);
						}
						return destinationFile;
					}
				});

				if (uncompressedData.isEmpty()) {
					if (logger.isWarnEnabled()) {
						logger.warn("No data unzipped from payload with message Id " + message.getHeaders().getId());
					}
					unzippedData = null;
				}
				else {

					if (this.expectSingleResult) {
						if (uncompressedData.size() == 1) {
							unzippedData = uncompressedData.values().iterator().next();
						}
						else {
							throw new MessagingException(message,
									String.format("The UnZip operation extracted %s "
											+ "result objects but expectSingleResult was 'true'.", uncompressedData.size()));
						}
					}
					else {
						unzippedData = uncompressedData;
					}

				}
			}
			finally {
				IOUtils.closeQuietly(inputStream);
				if (payload instanceof File && this.deleteFiles) {
					final File filePayload = (File) payload;
					if (!filePayload.delete() && logger.isWarnEnabled()) {
						if (logger.isWarnEnabled()) {
							logger.warn("failed to delete File '" + filePayload + "'");
						}
					}
				}
			}
			return unzippedData;
		}
		catch (Exception e) {
			throw new MessageHandlingException(message, "Failed to apply Zip transformation.", e);
		}
	}