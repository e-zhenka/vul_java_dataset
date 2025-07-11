public static void setHiveConfWhiteList(HiveConf hiveConf) throws HiveAuthzPluginException {

    String whiteListParamsStr = hiveConf
        .getVar(ConfVars.HIVE_AUTHORIZATION_SQL_STD_AUTH_CONFIG_WHITELIST);

    if(whiteListParamsStr == null || whiteListParamsStr.trim().isEmpty()) {
      throw new HiveAuthzPluginException("Configuration parameter "
          + ConfVars.HIVE_AUTHORIZATION_SQL_STD_AUTH_CONFIG_WHITELIST.varname
          + " is not initialized.");
    }

    // append regexes that user wanted to add
    String whiteListAppend = hiveConf
        .getVar(ConfVars.HIVE_AUTHORIZATION_SQL_STD_AUTH_CONFIG_WHITELIST_APPEND);
    if (whiteListAppend != null && !whiteListAppend.trim().equals("")) {
      whiteListParamsStr = whiteListParamsStr + "|" + whiteListAppend;
    }

    hiveConf.setModifiableWhiteListRegex(whiteListParamsStr);

    // disallow udfs that can potentially allow untrusted code execution
    // if admin has already customized this list, honor that
    String curBlackList = hiveConf.getVar(ConfVars.HIVE_SERVER2_BUILTIN_UDF_BLACKLIST);
    if (curBlackList == null || curBlackList.trim().isEmpty()) {
      hiveConf.setVar(ConfVars.HIVE_SERVER2_BUILTIN_UDF_BLACKLIST, "reflect,reflect2,java_method");
    }
  }