@WebMethod(name="heapdump.hprof")
        public void doHeapDump(StaplerRequest req, StaplerResponse rsp) throws IOException, InterruptedException {
            owner.checkPermission(Jenkins.RUN_SCRIPTS);
            rsp.setContentType("application/octet-stream");

            FilePath dump = obtain();
            try {
                dump.copyTo(rsp.getCompressedOutputStream(req));
            } finally {
                dump.delete();
            }
        }