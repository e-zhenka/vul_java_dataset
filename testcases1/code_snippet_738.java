private List<GHPoint> getPointsFromRequest(HttpServletRequest httpServletRequest, String profile) {
        String url = httpServletRequest.getRequestURI();
        String urlStart = "/navigate/directions/v5/gh/" + profile + "/";
        if (!url.startsWith(urlStart)) throw new IllegalArgumentException("Incorrect URL " + url);
        url = url.substring(urlStart.length());
        String[] pointStrings = url.split(";");
        List<GHPoint> points = new ArrayList<>(pointStrings.length);
        for (int i = 0; i < pointStrings.length; i++) {
            points.add(GHPoint.fromStringLonLat(pointStrings[i]));
        }

        return points;
    }