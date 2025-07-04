@Override
	protected void onInitialize()
	{
		super.onInitialize();

		this.textarea = new HiddenField<String>("textarea", this.getModel());
		this.textarea.setOutputMarkupId(true);
		this.textarea.setEscapeModelStrings(false);
		this.add(this.textarea);

		this.add(JQueryWidget.newWidgetBehavior(this, this.container));
	}