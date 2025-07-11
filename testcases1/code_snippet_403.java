private boolean instanceMethodParametersResolveToSameTypes(Method subTypeMethod, Method superTypeMethod) {
		if ( subTypeMethod.getParameterTypes().length == 0 ) {
			return true;
		}

		ResolvedType resolvedSubType = typeResolver.resolve( subTypeMethod.getDeclaringClass() );

		MemberResolver memberResolver = new MemberResolver( typeResolver );
		memberResolver.setMethodFilter( new SimpleMethodFilter( subTypeMethod, superTypeMethod ) );
		ResolvedTypeWithMembers typeWithMembers = memberResolver.resolve(
				resolvedSubType,
				null,
				null
		);

		// ClassMate itself doesn't require any special permissions, but it invokes reflection APIs which do.
		// Wrapping the call into a privileged action to avoid that all calling code bases need to have the required
		// permission
		ResolvedMethod[] resolvedMethods = run( GetResolvedMemberMethods.action( typeWithMembers ) );

		// The ClassMate doc says that overridden methods are flattened to one
		// resolved method. But that is the case only for methods without any
		// generic parameters.
		if ( resolvedMethods.length == 1 ) {
			return true;
		}

		// For methods with generic parameters I have to compare the argument
		// types (which are resolved) of the two filtered member methods.
		for ( int i = 0; i < resolvedMethods[0].getArgumentCount(); i++ ) {

			if ( !resolvedMethods[0].getArgumentType( i )
					.equals( resolvedMethods[1].getArgumentType( i ) ) ) {
				return false;
			}
		}

		return true;
	}