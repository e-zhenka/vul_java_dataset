public static int versionCompare(String fromVersion, String toVersion) {
        if (fromVersion == null || toVersion == null) {
            return -1;
        }
        String[] fromArr = fromVersion.split("\\.");
        String[] toArr = toVersion.split("\\.");
        if (fromArr.length != 3 || toArr.length != 3) {
            return -1;
        }
        try {
            int fromFirst = Integer.parseInt(fromArr[0]);
            int fromMiddle = Integer.parseInt(fromArr[1]);
            int fromEnd = Integer.parseInt(fromArr[2]);
            int toFirst = Integer.parseInt(toArr[0]);
            int toMiddle = Integer.parseInt(toArr[1]);
            int toEnd = Integer.parseInt(toArr[2]);
            if (fromFirst - toFirst != 0) {
                return fromFirst - toFirst;
            } else if (fromMiddle - toMiddle != 0) {
                return fromMiddle - toMiddle;
            } else {
                return fromEnd - toEnd;
            }
        } catch (NumberFormatException nfe) {
            return -1;
        }
    }