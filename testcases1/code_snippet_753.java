@Override
        public String getShortDescription() {
            if(note != null) {
                try {
                    return Messages.Cause_RemoteCause_ShortDescriptionWithNote(addr, Jenkins.getInstance().getMarkupFormatter().translate(note));
                } catch (IOException x) {
                    // ignore
                }
            }
            return Messages.Cause_RemoteCause_ShortDescription(addr);
        }