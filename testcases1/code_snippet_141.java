@Override
    public void log(org.apache.coyote.Request req,
            org.apache.coyote.Response res, long time) {

        Request request = (Request) req.getNote(ADAPTER_NOTES);
        Response response = (Response) res.getNote(ADAPTER_NOTES);
        boolean create = false;
        
        if (request == null) {
            create = true;
            // Create objects
            request = connector.createRequest();
            request.setCoyoteRequest(req);
            response = connector.createResponse();
            response.setCoyoteResponse(res);

            // Link objects
            request.setResponse(response);
            response.setRequest(request);

            // Set as notes
            req.setNote(ADAPTER_NOTES, request);
            res.setNote(ADAPTER_NOTES, response);

            // Set query string encoding
            req.getParameters().setQueryStringEncoding
                (connector.getURIEncoding());
        }
        
        try {
            connector.getService().getContainer().logAccess(
                    request, response, time, true);
        } catch (Throwable t) {
            ExceptionUtils.handleThrowable(t);
            log.warn(sm.getString("coyoteAdapter.accesslogFail"), t);
        }
        
        if (create) {
            request.recycle();
            response.recycle();
        }
    }