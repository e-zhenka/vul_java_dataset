private JWT decode(String encodedJWT, Header header, String[] parts, Verifier verifier) {
    // The callers of this decode will have already handled 'none' if it was deemed to be valid based upon
    // the provided verifiers. At this point, if we have a 'none' algorithm specified in the header, it is invalid.
    if (header.algorithm == Algorithm.none) {
      throw new MissingVerifierException("No Verifier has been provided for verify a signature signed using [" + header.algorithm.getName() + "]");
    }

    // If a signature is provided and verifier must be provided.
    if (parts.length == 3 && verifier == null) {
      throw new MissingVerifierException("No Verifier has been provided for verify a signature signed using [" + header.algorithm.getName() + "]");
    }

    // A verifier was provided but no signature exists, this is treated as an invalid signature.
    if (parts.length == 2 && verifier != null) {
      throw new InvalidJWTSignatureException();
    }

    int index = encodedJWT.lastIndexOf(".");
    // The message comprises the first two segments of the entire JWT, the signature is the last segment.
    byte[] message = encodedJWT.substring(0, index).getBytes(StandardCharsets.UTF_8);

    if (parts.length == 3) {
      // Verify the signature before de-serializing the payload.
      byte[] signature = base64Decode(parts[2].getBytes(StandardCharsets.UTF_8));
      verifier.verify(header.algorithm, message, signature);
    }

    JWT jwt = Mapper.deserialize(base64Decode(parts[1].getBytes(StandardCharsets.UTF_8)), JWT.class);

    // Verify expiration claim
    if (jwt.isExpired()) {
      throw new JWTExpiredException();
    }

    // Verify the notBefore claim
    if (jwt.isUnavailableForProcessing()) {
      throw new JWTUnavailableForProcessingException();
    }

    return jwt;
  }