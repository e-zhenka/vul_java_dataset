public String getIconFileName() {
        return Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER) ? "plugin/jenkins-multijob-plugin/tool32.png" : null;
	}