@Override
    public String getWhyKeepLog() {
        // if any of the downstream project is configured with 'keep dependency component',
        // we need to keep this log
        OUTER:
        for (AbstractProject<?,?> p : getParent().getDownstreamProjects()) {
            if (!p.isKeepDependencies()) continue;

            AbstractBuild<?,?> fb = p.getFirstBuild();
            if (fb==null)        continue; // no active record

            // is there any active build that depends on us?
            for (int i : getDownstreamRelationship(p).listNumbersReverse()) {
                // TODO: this is essentially a "find intersection between two sparse sequences"
                // and we should be able to do much better.

                if (i<fb.getNumber())
                    continue OUTER; // all the other records are younger than the first record, so pointless to search.

                AbstractBuild<?,?> b = p.getBuildByNumber(i);
                if (b!=null)
                    return Messages.AbstractBuild_KeptBecause(p.hasPermission(Item.READ) ? b.toString() : "?");
            }
        }

        return super.getWhyKeepLog();
    }