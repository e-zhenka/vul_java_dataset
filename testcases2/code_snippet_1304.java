private static PropertyPath create(String source, TypeInformation<?> type, String addTail, List<PropertyPath> base) {

		if (base.size() > 1000) {
			throw new IllegalArgumentException(PARSE_DEPTH_EXCEEDED);
		}

		PropertyReferenceException exception = null;
		PropertyPath current = null;

		try {

			current = new PropertyPath(source, type, base);

			if (!base.isEmpty()) {
				base.get(base.size() - 1).next = current;
			}

			List<PropertyPath> newBase = new ArrayList<PropertyPath>(base);
			newBase.add(current);

			if (StringUtils.hasText(addTail)) {
				current.next = create(addTail, current.type, newBase);
			}

			return current;

		} catch (PropertyReferenceException e) {

			if (current != null) {
				throw e;
			}

			exception = e;
		}

		Pattern pattern = Pattern.compile("\\p{Lu}+\\p{Ll}*$");
		Matcher matcher = pattern.matcher(source);

		if (matcher.find() && matcher.start() != 0) {

			int position = matcher.start();
			String head = source.substring(0, position);
			String tail = source.substring(position);

			try {
				return create(head, type, tail + addTail, base);
			} catch (PropertyReferenceException e) {
				throw e.hasDeeperResolutionDepthThan(exception) ? e : exception;
			}
		}

		throw exception;
	}