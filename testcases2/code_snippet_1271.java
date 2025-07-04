public FormValidation doCheckCommand(@QueryParameter String value) {
            if (!Jenkins.getInstance().hasPermission(Jenkins.RUN_SCRIPTS)) {
                return FormValidation.warning(Messages.CommandLauncher_cannot_be_configured_by_non_administrato());
            }
            if(Util.fixEmptyAndTrim(value)==null)
                return FormValidation.error(Messages.CommandLauncher_NoLaunchCommand());
            else
                return FormValidation.ok();
        }