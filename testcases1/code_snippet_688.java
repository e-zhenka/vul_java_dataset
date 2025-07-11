private static CloseableHttpClient getAllTrustClient(HttpHost proxy) {
			try {
				HttpClientBuilder clientBuilder = HttpClientBuilder.create();
				SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
					public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
						return true;
					}
				}).build();
				clientBuilder.setSSLContext(sslContext);
				
				HostnameVerifier hostnameVerifier = new NoopHostnameVerifier();
	
				SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
				Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
						.register("http", PlainConnectionSocketFactory.getSocketFactory())
						.register("https", sslSocketFactory)
						.build();
			 
				PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager( socketFactoryRegistry);
				clientBuilder.setConnectionManager(connMgr);
			 
				return clientBuilder.build();
			} catch (GeneralSecurityException e) {
				// shouldn't happen
				throw new RuntimeException(e);
			}
		}