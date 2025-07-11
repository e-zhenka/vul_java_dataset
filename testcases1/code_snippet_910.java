@SuppressWarnings("ACL.impersonate")
    private void loginAndTakeBack(StaplerRequest req, StaplerResponse rsp, User u) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            // avoid session fixation
            session.invalidate();
        }
        req.getSession(true);
        
        // ... and let him login
        Authentication a = new UsernamePasswordAuthenticationToken(u.getId(),req.getParameter("password1"));
        a = this.getSecurityComponents().manager.authenticate(a);
        SecurityContextHolder.getContext().setAuthentication(a);

        // then back to top
        req.getView(this,"success.jelly").forward(req,rsp);
    }