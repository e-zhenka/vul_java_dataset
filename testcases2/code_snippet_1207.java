protected boolean acceptableName(String name) {
        return isWithinLengthLimit(name) && isAccepted(name)
                && !isExcluded(name);
    }