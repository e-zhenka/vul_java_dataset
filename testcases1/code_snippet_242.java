@Override
    public SecurityConstraint [] findSecurityConstraints(Request request,
                                                         Context context) {

        ArrayList<SecurityConstraint> results = null;
        // Are there any defined security constraints?
        SecurityConstraint constraints[] = context.findConstraints();
        if ((constraints == null) || (constraints.length == 0)) {
            if (log.isDebugEnabled())
                log.debug("  No applicable constraints defined");
            return null;
        }

        // Check each defined security constraint
        String uri = request.getRequestPathMB().toString();
        // Bug47080 - in rare cases this may be null
        // Mapper treats as '/' do the same to prevent NPE
        if (uri == null) {
            uri = "/";
        }

        String method = request.getMethod();
        int i;
        boolean found = false;
        for (i = 0; i < constraints.length; i++) {
            SecurityCollection [] collection = constraints[i].findCollections();

            // If collection is null, continue to avoid an NPE
            // See Bugzilla 30624
            if ( collection == null) {
                continue;
            }

            if (log.isDebugEnabled()) {
                log.debug("  Checking constraint '" + constraints[i] +
                    "' against " + method + " " + uri + " --> " +
                    constraints[i].included(uri, method));
            }

            for(int j=0; j < collection.length; j++){
                String [] patterns = collection[j].findPatterns();

                // If patterns is null, continue to avoid an NPE
                // See Bugzilla 30624
                if ( patterns == null) {
                    continue;
                }

                for(int k=0; k < patterns.length; k++) {
                    if(uri.equals(patterns[k])) {
                        found = true;
                        if(collection[j].findMethod(method)) {
                            if(results == null) {
                                results = new ArrayList<>();
                            }
                            results.add(constraints[i]);
                        }
                    }
                }
            }
        }

        if(found) {
            return resultsToArray(results);
        }

        int longest = -1;

        for (i = 0; i < constraints.length; i++) {
            SecurityCollection [] collection = constraints[i].findCollections();

            // If collection is null, continue to avoid an NPE
            // See Bugzilla 30624
            if ( collection == null) {
                continue;
            }

            if (log.isDebugEnabled()) {
                log.debug("  Checking constraint '" + constraints[i] +
                    "' against " + method + " " + uri + " --> " +
                    constraints[i].included(uri, method));
            }

            for(int j=0; j < collection.length; j++){
                String [] patterns = collection[j].findPatterns();

                // If patterns is null, continue to avoid an NPE
                // See Bugzilla 30624
                if ( patterns == null) {
                    continue;
                }

                boolean matched = false;
                int length = -1;
                for(int k=0; k < patterns.length; k++) {
                    String pattern = patterns[k];
                    if(pattern.startsWith("/") && pattern.endsWith("/*") &&
                       pattern.length() >= longest) {

                        if(pattern.length() == 2) {
                            matched = true;
                            length = pattern.length();
                        } else if(pattern.regionMatches(0,uri,0,
                                                        pattern.length()-1) ||
                                  (pattern.length()-2 == uri.length() &&
                                   pattern.regionMatches(0,uri,0,
                                                        pattern.length()-2))) {
                            matched = true;
                            length = pattern.length();
                        }
                    }
                }
                if(matched) {
                    if(length > longest) {
                        found = false;
                        if(results != null) {
                            results.clear();
                        }
                        longest = length;
                    }
                    if(collection[j].findMethod(method)) {
                        found = true;
                        if(results == null) {
                            results = new ArrayList<>();
                        }
                        results.add(constraints[i]);
                    }
                }
            }
        }

        if(found) {
            return  resultsToArray(results);
        }

        for (i = 0; i < constraints.length; i++) {
            SecurityCollection [] collection = constraints[i].findCollections();

            // If collection is null, continue to avoid an NPE
            // See Bugzilla 30624
            if ( collection == null) {
                continue;
            }

            if (log.isDebugEnabled()) {
                log.debug("  Checking constraint '" + constraints[i] +
                    "' against " + method + " " + uri + " --> " +
                    constraints[i].included(uri, method));
            }

            boolean matched = false;
            int pos = -1;
            for(int j=0; j < collection.length; j++){
                String [] patterns = collection[j].findPatterns();

                // If patterns is null, continue to avoid an NPE
                // See Bugzilla 30624
                if ( patterns == null) {
                    continue;
                }

                for(int k=0; k < patterns.length && !matched; k++) {
                    String pattern = patterns[k];
                    if(pattern.startsWith("*.")){
                        int slash = uri.lastIndexOf('/');
                        int dot = uri.lastIndexOf('.');
                        if(slash >= 0 && dot > slash &&
                           dot != uri.length()-1 &&
                           uri.length()-dot == pattern.length()-1) {
                            if(pattern.regionMatches(1,uri,dot,uri.length()-dot)) {
                                matched = true;
                                pos = j;
                            }
                        }
                    }
                }
            }
            if(matched) {
                found = true;
                if(collection[pos].findMethod(method)) {
                    if(results == null) {
                        results = new ArrayList<>();
                    }
                    results.add(constraints[i]);
                }
            }
        }

        if(found) {
            return resultsToArray(results);
        }

        for (i = 0; i < constraints.length; i++) {
            SecurityCollection [] collection = constraints[i].findCollections();

            // If collection is null, continue to avoid an NPE
            // See Bugzilla 30624
            if ( collection == null) {
                continue;
            }

            if (log.isDebugEnabled()) {
                log.debug("  Checking constraint '" + constraints[i] +
                    "' against " + method + " " + uri + " --> " +
                    constraints[i].included(uri, method));
            }

            for(int j=0; j < collection.length; j++){
                String [] patterns = collection[j].findPatterns();

                // If patterns is null, continue to avoid an NPE
                // See Bugzilla 30624
                if ( patterns == null) {
                    continue;
                }

                boolean matched = false;
                for(int k=0; k < patterns.length && !matched; k++) {
                    String pattern = patterns[k];
                    if(pattern.equals("/")){
                        matched = true;
                    }
                }
                if(matched) {
                    if(results == null) {
                        results = new ArrayList<>();
                    }
                    results.add(constraints[i]);
                }
            }
        }

        if(results == null) {
            // No applicable security constraint was found
            if (log.isDebugEnabled())
                log.debug("  No applicable constraint located");
        }
        return resultsToArray(results);
    }