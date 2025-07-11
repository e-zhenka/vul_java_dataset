protected void setUpSSL() throws Exception {
		if (this.sslPropertiesLocation == null && this.keyStore == null && this.trustStore == null
				&& this.keyStoreResource == null && this.trustStoreResource == null) {
			if (this.skipServerCertificateValidation) {
				if (this.sslAlgorithmSet) {
					this.connectionFactory.useSslProtocol(this.sslAlgorithm);
				}
				else {
					this.connectionFactory.useSslProtocol();
				}
			}
			else {
				useDefaultTrustStoreMechanism();
			}
		}
		else {
			if (this.sslPropertiesLocation != null) {
				this.sslProperties.load(this.sslPropertiesLocation.getInputStream());
			}
			PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
			String keyStoreName = getKeyStore();
			String trustStoreName = getTrustStore();
			String keyStorePassword = getKeyStorePassphrase();
			String trustStorePassword = getTrustStorePassphrase();
			String keyStoreType = getKeyStoreType();
			String trustStoreType = getTrustStoreType();
			char[] keyPassphrase = null;
			if (keyStorePassword != null) {
				keyPassphrase = keyStorePassword.toCharArray();
			}
			char[] trustPassphrase = null;
			if (trustStorePassword != null) {
				trustPassphrase = trustStorePassword.toCharArray();
			}
			KeyManager[] keyManagers = null;
			TrustManager[] trustManagers = null;
			if (StringUtils.hasText(keyStoreName) || this.keyStoreResource != null) {
				Resource keyStoreResource = this.keyStoreResource != null ? this.keyStoreResource
						: resolver.getResource(keyStoreName);
				KeyStore ks = KeyStore.getInstance(keyStoreType);
				ks.load(keyStoreResource.getInputStream(), keyPassphrase);
				KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
				kmf.init(ks, keyPassphrase);
				keyManagers = kmf.getKeyManagers();
			}
			if (StringUtils.hasText(trustStoreName) || this.trustStoreResource != null) {
				Resource trustStoreResource = this.trustStoreResource != null ? this.trustStoreResource
						: resolver.getResource(trustStoreName);
				KeyStore tks = KeyStore.getInstance(trustStoreType);
				tks.load(trustStoreResource.getInputStream(), trustPassphrase);
				TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
				tmf.init(tks);
				trustManagers = tmf.getTrustManagers();
			}

			if (this.logger.isDebugEnabled()) {
				this.logger.debug("Initializing SSLContext with KM: "
						+ Arrays.toString(keyManagers)
						+ ", TM: " + Arrays.toString(trustManagers)
						+ ", random: " + this.secureRandom);
			}
			SSLContext context = createSSLContext();
			context.init(keyManagers, trustManagers, this.secureRandom);
			this.connectionFactory.useSslProtocol(context);
		}
	}