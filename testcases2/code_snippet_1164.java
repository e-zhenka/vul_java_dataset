protected final String getWindowOpenJavaScript()
	{
		AppendingStringBuffer buffer = new AppendingStringBuffer(500);

		if (isCustomComponent() == true)
		{
			buffer.append("var element = document.getElementById(\"");
			buffer.append(getContentMarkupId());
			buffer.append("\");\n");
		}

		buffer.append("var settings = new Object();\n");

		appendAssignment(buffer, "settings.minWidth", getMinimalWidth());
		appendAssignment(buffer, "settings.minHeight", getMinimalHeight());
		appendAssignment(buffer, "settings.className", getCssClassName());
		appendAssignment(buffer, "settings.width", getInitialWidth());

		if ((isUseInitialHeight() == true) || (isCustomComponent() == false))
		{
			appendAssignment(buffer, "settings.height", getInitialHeight());
		}
		else
		{
			buffer.append("settings.height=null;\n");
		}

		appendAssignment(buffer, "settings.resizable", isResizable());

		if (isResizable() == false)
		{
			appendAssignment(buffer, "settings.widthUnit", getWidthUnit());
			appendAssignment(buffer, "settings.heightUnit", getHeightUnit());
		}

		if (isCustomComponent() == false)
		{
			Page page = createPage();
			if (page == null)
			{
				throw new WicketRuntimeException("Error creating page for modal dialog.");
			}
			CharSequence pageUrl;
			RequestCycle requestCycle = RequestCycle.get();

			page.getSession().getPageManager().touchPage(page);
			if (page.isPageStateless())
			{
				pageUrl = requestCycle.urlFor(page.getClass(), page.getPageParameters());
			}
			else
			{
				IRequestHandler handler = new RenderPageRequestHandler(new PageProvider(page));
				pageUrl = requestCycle.urlFor(handler);
			}

			appendAssignment(buffer, "settings.src", pageUrl);
		}
		else
		{
			buffer.append("settings.element=element;\n");
		}

		if (getCookieName() != null)
		{
			appendAssignment(buffer, "settings.cookieId", getCookieName());
		}

		String title = getTitle() != null ? getTitle().getObject() : null;
		if (title != null)
		{
			String escaped = getDefaultModelObjectAsString(title);
			appendAssignment(buffer, "settings.title", escaped);
		}

		if (getMaskType() == MaskType.TRANSPARENT)
		{
			buffer.append("settings.mask=\"transparent\";\n");
		}
		else if (getMaskType() == MaskType.SEMI_TRANSPARENT)
		{
			buffer.append("settings.mask=\"semi-transparent\";\n");
		}

		appendAssignment(buffer, "settings.autoSize", autoSize);

		appendAssignment(buffer, "settings.unloadConfirmation", showUnloadConfirmation());

		// set true if we set a windowclosedcallback
		boolean haveCloseCallback = false;

		// in case user is interested in window close callback or we have a pagemap to clean attach
		// notification request
		if (windowClosedCallback != null)
		{
			WindowClosedBehavior behavior = getBehaviors(WindowClosedBehavior.class).get(0);
			buffer.append("settings.onClose = function() { ");
			buffer.append(behavior.getCallbackScript());
			buffer.append(" };\n");

			haveCloseCallback = true;
		}

		// in case we didn't set windowclosecallback, we need at least callback on close button, to
		// close window property (thus cleaning the shown flag)
		if ((closeButtonCallback != null) || (haveCloseCallback == false))
		{
			CloseButtonBehavior behavior = getBehaviors(CloseButtonBehavior.class).get(0);
			buffer.append("settings.onCloseButton = function() { ");
			buffer.append(behavior.getCallbackScript());
			buffer.append(";return false;};\n");
		}

		postProcessSettings(buffer);

		buffer.append(getShowJavaScript());
		return buffer.toString();
	}