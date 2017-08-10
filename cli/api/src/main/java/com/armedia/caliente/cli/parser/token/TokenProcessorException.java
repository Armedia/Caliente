package com.armedia.caliente.cli.parser.token;

import com.armedia.caliente.cli.CommandLineException;

public class TokenProcessorException extends CommandLineException {
	private static final long serialVersionUID = 1L;

	public TokenProcessorException() {
		super();
	}

	public TokenProcessorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public TokenProcessorException(String message, Throwable cause) {
		super(message, cause);
	}

	public TokenProcessorException(String message) {
		super(message);
	}

	public TokenProcessorException(Throwable cause) {
		super(cause);
	}
}