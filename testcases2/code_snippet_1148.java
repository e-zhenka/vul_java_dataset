protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter out = response.getWriter();
        String[] stocks = request.getParameterValues("stocks");
        if (stocks == null || stocks.length == 0) {
            out.println("<html><body>No <b>stocks</b> query parameter specified. Cannot publish market data</body></html>");
        } else {
            Integer total = (Integer)request.getSession(true).getAttribute("total");
            if (total == null) {
                total = Integer.valueOf(0);
            }

            int count = getNumberOfMessages(request);
            total = Integer.valueOf(total.intValue() + count);
            request.getSession().setAttribute("total", total);

            try {
                WebClient client = WebClient.getWebClient(request);
                for (int i = 0; i < count; i++) {
                    sendMessage(client, stocks);
                }
                out.print("<html><head><meta http-equiv='refresh' content='");
                String refreshRate = request.getParameter("refresh");
                if (refreshRate == null || refreshRate.length() == 0) {
                    refreshRate = "1";
                }
                out.print(refreshRate);
                out.println("'/></head>");
                out.println("<body>Published <b>" + count + "</b> of " + total + " price messages.  Refresh = " + refreshRate + "s");
                out.println("</body></html>");

            } catch (JMSException e) {
                out.println("<html><body>Failed sending price messages due to <b>" + e + "</b></body></html>");
                log("Failed to send message: " + e, e);
            }
        }
    }