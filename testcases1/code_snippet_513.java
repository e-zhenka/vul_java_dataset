public HttpResponse doForcePromotion(@QueryParameter("name") String name) throws IOException {
//        if(!req.getMethod().equals("POST")) {// require post,
//            rsp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
//            return;
//        }

        JobPropertyImpl pp = getProject().getProperty(JobPropertyImpl.class);
        if(pp==null)
            throw new IllegalStateException("This project doesn't have any promotion criteria set");

        PromotionProcess p = pp.getItem(name);
        if(p==null)
            throw new IllegalStateException("This project doesn't have the promotion criterion called "+name);

        ManualCondition manualCondition = (ManualCondition) p.getPromotionCondition(ManualCondition.class.getName());
        PromotionPermissionHelper.checkPermission(getProject(), manualCondition);

        p.promote(owner,new UserCause(),new ManualPromotionBadge());

        return HttpResponses.redirectToDot();
    }