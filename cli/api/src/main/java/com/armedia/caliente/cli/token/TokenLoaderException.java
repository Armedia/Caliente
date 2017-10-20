package com.armedia.caliente.cli.token;

import com.armedia.caliente.cli.exception.CommandLineException;

public class TokenLoaderException extends CommandLineException {
	private static final long serialVersionUID = 1L;

	public TokenLoaderException() {
		super();
	}

	public TokenLoaderException(String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public TokenLoaderException(String message, Throwable cause) {
		super(message, cause);
	}

	public TokenLoaderException(String message) {
		super(message);
	}

	public TokenLoaderException(Throwable cause) {
		super(cause);
	}
}