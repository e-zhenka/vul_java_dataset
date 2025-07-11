public static void enableDefaultTyping(ObjectMapper mapper) {
		if(mapper != null) {
			TypeResolverBuilder<?> typeBuilder = mapper.getDeserializationConfig().getDefaultTyper(null);
			if (typeBuilder == null) {
				mapper.setDefaultTyping(createWhitelistedDefaultTyping());
			}
		}
	}