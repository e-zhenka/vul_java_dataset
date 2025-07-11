PublicKey getRemotePublicKey(String nodename) {
    String url = cores.getZkController().getZkStateReader().getBaseUrlForNodeName(nodename);
    try {
      String uri = url + PATH + "?wt=json&omitHeader=true";
      log.debug("Fetching fresh public key from : {}",uri);
      HttpResponse rsp = cores.getUpdateShardHandler().getHttpClient().execute(new HttpGet(uri));
      byte[] bytes = EntityUtils.toByteArray(rsp.getEntity());
      Map m = (Map) Utils.fromJSON(bytes);
      String key = (String) m.get("key");
      if (key == null) {
        log.error("No key available from " + url + PATH);
        return null;
      } else {
        log.info("New Key obtained from  node: {} / {}", nodename, key);
      }
      PublicKey pubKey = CryptoKeys.deserializeX509PublicKey(key);
      keyCache.put(nodename, pubKey);
      return pubKey;
    } catch (Exception e) {
      log.error("Exception trying to get public key from : " + url, e);
      return null;
    }

  }