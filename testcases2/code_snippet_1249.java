@Override
	public void convertInput()
	{
		final PolicyFactory policy = newPolicyFactory();
		final String input = this.textarea.getConvertedInput();

		this.setConvertedInput(policy.sanitize(input));
	}