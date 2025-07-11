@Override
	public OAuth2TokenValidatorResult validate(Jwt token) {
		Assert.notNull(token, "token cannot be null");

		String tokenIssuer = token.getClaimAsString(JwtClaimNames.ISS);
		if (this.issuer.equals(tokenIssuer)) {
			return OAuth2TokenValidatorResult.success();
		} else {
			return OAuth2TokenValidatorResult.failure(INVALID_ISSUER);
		}
	}