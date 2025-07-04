private List<GHPoint> getPointsFromRequest(HttpServletRequest httpServletRequest, String profile) {

        String url = httpServletRequest.getRequestURI();
        url = url.replaceFirst("/navigate/directions/v5/gh/" + profile + "/", "");
        url = url.replaceAll("\\?[*]", "");

        String[] pointStrings = url.split(";");

        List<GHPoint> points = new ArrayList<>(pointStrings.length);
        for (int i = 0; i < pointStrings.length; i++) {
            points.add(GHPoint.fromStringLonLat(pointStrings[i]));
        }

        return points;
    }