public SecurityWebFilterChain build() {
		if(this.built != null) {
			throw new IllegalStateException("This has already been built with the following stacktrace. " + buildToString());
		}
		this.built = new RuntimeException("First Build Invocation").fillInStackTrace();
		if(this.headers != null) {
			this.headers.configure(this);
		}
		WebFilter securityContextRepositoryWebFilter = securityContextRepositoryWebFilter();
		if(securityContextRepositoryWebFilter != null) {
			this.webFilters.add(securityContextRepositoryWebFilter);
		}
		if(this.csrf != null) {
			this.csrf.configure(this);
		}
		if(this.httpBasic != null) {
			this.httpBasic.authenticationManager(this.authenticationManager);
			this.httpBasic.configure(this);
		}
		if(this.formLogin != null) {
			this.formLogin.authenticationManager(this.authenticationManager);
			if(this.securityContextRepository != null) {
				this.formLogin.securityContextRepository(this.securityContextRepository);
			}
			if(this.formLogin.authenticationEntryPoint == null) {
				this.webFilters.add(new OrderedWebFilter(new LoginPageGeneratingWebFilter(), SecurityWebFiltersOrder.LOGIN_PAGE_GENERATING.getOrder()));
				this.webFilters.add(new OrderedWebFilter(new LogoutPageGeneratingWebFilter(), SecurityWebFiltersOrder.LOGOUT_PAGE_GENERATING.getOrder()));
			}
			this.formLogin.configure(this);
		}
		if(this.logout != null) {
			this.logout.configure(this);
		}
		this.requestCache.configure(this);
		this.addFilterAt(new SecurityContextServerWebExchangeWebFilter(), SecurityWebFiltersOrder.SECURITY_CONTEXT_SERVER_WEB_EXCHANGE);
		if(this.authorizeExchange != null) {
			ServerAuthenticationEntryPoint authenticationEntryPoint = getAuthenticationEntryPoint();
			ExceptionTranslationWebFilter exceptionTranslationWebFilter = new ExceptionTranslationWebFilter();
			if(authenticationEntryPoint != null) {
				exceptionTranslationWebFilter.setAuthenticationEntryPoint(
					authenticationEntryPoint);
			}
			this.addFilterAt(exceptionTranslationWebFilter, SecurityWebFiltersOrder.EXCEPTION_TRANSLATION);
			this.authorizeExchange.configure(this);
		}
		AnnotationAwareOrderComparator.sort(this.webFilters);
		List<WebFilter> sortedWebFilters = new ArrayList<>();
		this.webFilters.forEach( f -> {
			if(f instanceof OrderedWebFilter) {
				f = ((OrderedWebFilter) f).webFilter;
			}
			sortedWebFilters.add(f);
		});
		return new MatcherSecurityWebFilterChain(getSecurityMatcher(), sortedWebFilters);
	}