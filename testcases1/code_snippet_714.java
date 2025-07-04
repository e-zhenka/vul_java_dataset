@Deprecated
    @SuppressFBWarnings(
            value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE",
            justification = "False positive for try-with-resources in Java 11")
    public void doSlaveInfo(StaplerRequest req, StaplerResponse rsp) throws IOException {
        Jenkins jenkins = Jenkins.get();
        jenkins.checkPermission(SlaveComputer.CREATE);

        rsp.setContentType("text/xml");
        try (Writer w = rsp.getCompressedWriter(req)) {
            w.write("<slaveInfo><swarmSecret>" + UUID.randomUUID().toString() + "</swarmSecret></slaveInfo>");
        }
    }