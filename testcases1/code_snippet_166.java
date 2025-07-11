@Override
    public boolean willAttributeDistribute(String name, Object value) {
        Pattern sessionAttributeNamePattern = getSessionAttributeNamePattern();
        if (sessionAttributeNamePattern != null) {
            if (!sessionAttributeNamePattern.matcher(name).matches()) {
                if (getWarnOnSessionAttributeFilterFailure() || log.isDebugEnabled()) {
                    String msg = sm.getString("managerBase.sessionAttributeNameFilter",
                            name, sessionAttributeNamePattern);
                    if (getWarnOnSessionAttributeFilterFailure()) {
                        log.warn(msg);
                    } else {
                        log.debug(msg);
                    }
                }
                return false;
            }
        }

        Pattern sessionAttributeValueClassNamePattern = getSessionAttributeValueClassNamePattern();
        if (value != null && sessionAttributeValueClassNamePattern != null) {
            if (!sessionAttributeValueClassNamePattern.matcher(
                    value.getClass().getName()).matches()) {
                if (getWarnOnSessionAttributeFilterFailure() || log.isDebugEnabled()) {
                    String msg = sm.getString("managerBase.sessionAttributeValueClassNameFilter",
                            name, value.getClass().getName(), sessionAttributeNamePattern);
                    if (getWarnOnSessionAttributeFilterFailure()) {
                        log.warn(msg);
                    } else {
                        log.debug(msg);
                    }
                }
                return false;
            }
        }

        return true;
    }