package com.armedia.caliente.cli.token;

import com.armedia.caliente.cli.CommandLineException;

public class TokenResolverException extends CommandLineException {
	private static final long serialVersionUID = 1L;

	public TokenResolverException() {
		super();
	}

	public TokenResolverException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public TokenResolverException(String message, Throwable cause) {
		super(message, cause);
	}

	public TokenResolverException(String message) {
		super(message);
	}

	public TokenResolverException(Throwable cause) {
		super(cause);
	}
}