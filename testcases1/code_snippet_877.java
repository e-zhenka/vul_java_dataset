public void list(HttpServletRequest request,
                     HttpServletResponse response,
                     String message) throws IOException {

        PrintWriter writer = response.getWriter();

        // HTML Header Section
        writer.print(Constants.HTML_HEADER_SECTION);

        // Body Header Section
        Object[] args = new Object[2];
        args[0] = request.getContextPath();
        args[1] = sm.getString("htmlHostManagerServlet.title");
        writer.print(MessageFormat.format
                     (Constants.BODY_HEADER_SECTION, args));

        // Message Section
        args = new Object[3];
        args[0] = sm.getString("htmlHostManagerServlet.messageLabel");
        if (message == null || message.length() == 0) {
            args[1] = "OK";
        } else {
            args[1] = RequestUtil.filter(message);
        }
        writer.print(MessageFormat.format(Constants.MESSAGE_SECTION, args));

        // Manager Section
        args = new Object[9];
        args[0] = sm.getString("htmlHostManagerServlet.manager");
        args[1] = response.encodeURL(request.getContextPath() + "/html/list");
        args[2] = sm.getString("htmlHostManagerServlet.list");
        args[3] = response.encodeURL
            (request.getContextPath() + "/" +
             sm.getString("htmlHostManagerServlet.helpHtmlManagerFile"));
        args[4] = sm.getString("htmlHostManagerServlet.helpHtmlManager");
        args[5] = response.encodeURL
            (request.getContextPath() + "/" +
             sm.getString("htmlHostManagerServlet.helpManagerFile"));
        args[6] = sm.getString("htmlHostManagerServlet.helpManager");
        args[7] = response.encodeURL("/manager/status");
        args[8] = sm.getString("statusServlet.title");
        writer.print(MessageFormat.format(Constants.MANAGER_SECTION, args));

         // Hosts Header Section
        args = new Object[3];
        args[0] = sm.getString("htmlHostManagerServlet.hostName");
        args[1] = sm.getString("htmlHostManagerServlet.hostAliases");
        args[2] = sm.getString("htmlHostManagerServlet.hostTasks");
        writer.print(MessageFormat.format(HOSTS_HEADER_SECTION, args));

        // Hosts Row Section
        // Create sorted map of host names.
        Container[] children = engine.findChildren();
        String hostNames[] = new String[children.length];
        for (int i = 0; i < children.length; i++)
            hostNames[i] = children[i].getName();

        TreeMap sortedHostNamesMap = new TreeMap();

        for (int i = 0; i < hostNames.length; i++) {
            String displayPath = hostNames[i];
            sortedHostNamesMap.put(displayPath, hostNames[i]);
        }

        String hostsStart = sm.getString("htmlHostManagerServlet.hostsStart");
        String hostsStop = sm.getString("htmlHostManagerServlet.hostsStop");
        String hostsRemove = sm.getString("htmlHostManagerServlet.hostsRemove");

        Iterator iterator = sortedHostNamesMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String hostName = (String) entry.getKey();
            Host host = (Host) engine.findChild(hostName);

            if (host != null ) {
                args = new Object[2];
                args[0] = hostName;
                String[] aliases = host.findAliases();
                StringBuffer buf = new StringBuffer();
                if (aliases.length > 0) {
                    buf.append(aliases[0]);
                    for (int j = 1; j < aliases.length; j++) {
                        buf.append(", ").append(aliases[j]);
                    }
                }

                if (buf.length() == 0) {
                    buf.append("&nbsp;");
                }

                args[1] = buf.toString();
                writer.print
                    (MessageFormat.format(HOSTS_ROW_DETAILS_SECTION, args));

                args = new Object[7];
                args[0] = response.encodeURL
                    (request.getContextPath() +
                     "/html/start?name=" + hostName);
                args[1] = hostsStart;
                args[2] = response.encodeURL
                    (request.getContextPath() +
                     "/html/stop?name=" + hostName);
                args[3] = hostsStop;
                args[4] = response.encodeURL
                    (request.getContextPath() +
                     "/html/remove?name=" + hostName);
                args[5] = hostsRemove;
                args[6] = hostName;
                if (host == this.host) {
                    writer.print(MessageFormat.format(
                        MANAGER_HOST_ROW_BUTTON_SECTION, args));
                } else {
                    writer.print(MessageFormat.format(
                        HOSTS_ROW_BUTTON_SECTION, args));
                }

            }
        }

        // Add Section
        args = new Object[6];
        args[0] = sm.getString("htmlHostManagerServlet.addTitle");
        args[1] = sm.getString("htmlHostManagerServlet.addHost");
        args[2] = response.encodeURL(request.getContextPath() + "/html/add");
        args[3] = sm.getString("htmlHostManagerServlet.addName");
        args[4] = sm.getString("htmlHostManagerServlet.addAliases");
        args[5] = sm.getString("htmlHostManagerServlet.addAppBase");
        writer.print(MessageFormat.format(ADD_SECTION_START, args));
 
        args = new Object[3];
        args[0] = sm.getString("htmlHostManagerServlet.addAutoDeploy");
        args[1] = "autoDeploy";
        args[2] = "checked";
        writer.print(MessageFormat.format(ADD_SECTION_BOOLEAN, args));
        args[0] = sm.getString("htmlHostManagerServlet.addDeployOnStartup");
        args[1] = "deployOnStartup";
        args[2] = "checked";
        writer.print(MessageFormat.format(ADD_SECTION_BOOLEAN, args));
        args[0] = sm.getString("htmlHostManagerServlet.addDeployXML");
        args[1] = "deployXML";
        args[2] = "checked";
        writer.print(MessageFormat.format(ADD_SECTION_BOOLEAN, args));
        args[0] = sm.getString("htmlHostManagerServlet.addUnpackWARs");
        args[1] = "unpackWARs";
        args[2] = "checked";
        writer.print(MessageFormat.format(ADD_SECTION_BOOLEAN, args));
        args[0] = sm.getString("htmlHostManagerServlet.addXmlNamespaceAware");
        args[1] = "xmlNamespaceAware";
        args[2] = "";
        writer.print(MessageFormat.format(ADD_SECTION_BOOLEAN, args));
        args[0] = sm.getString("htmlHostManagerServlet.addXmlValidation");
        args[1] = "xmlValidation";
        args[2] = "";
        writer.print(MessageFormat.format(ADD_SECTION_BOOLEAN, args));

        args[0] = sm.getString("htmlHostManagerServlet.addManager");
        args[1] = "manager";
        args[2] = "checked";
        writer.print(MessageFormat.format(ADD_SECTION_BOOLEAN, args));
        
        args = new Object[1];
        args[0] = sm.getString("htmlHostManagerServlet.addButton");
        writer.print(MessageFormat.format(ADD_SECTION_END, args));

        // Server Header Section
        args = new Object[7];
        args[0] = sm.getString("htmlHostManagerServlet.serverTitle");
        args[1] = sm.getString("htmlHostManagerServlet.serverVersion");
        args[2] = sm.getString("htmlHostManagerServlet.serverJVMVersion");
        args[3] = sm.getString("htmlHostManagerServlet.serverJVMVendor");
        args[4] = sm.getString("htmlHostManagerServlet.serverOSName");
        args[5] = sm.getString("htmlHostManagerServlet.serverOSVersion");
        args[6] = sm.getString("htmlHostManagerServlet.serverOSArch");
        writer.print(MessageFormat.format
                     (Constants.SERVER_HEADER_SECTION, args));

        // Server Row Section
        args = new Object[6];
        args[0] = ServerInfo.getServerInfo();
        args[1] = System.getProperty("java.runtime.version");
        args[2] = System.getProperty("java.vm.vendor");
        args[3] = System.getProperty("os.name");
        args[4] = System.getProperty("os.version");
        args[5] = System.getProperty("os.arch");
        writer.print(MessageFormat.format(Constants.SERVER_ROW_SECTION, args));

        // HTML Tail Section
        writer.print(Constants.HTML_TAIL_SECTION);

        // Finish up the response
        writer.flush();
        writer.close();
    }