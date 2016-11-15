package com.armedia.caliente.cli.parser;

import com.armedia.caliente.cli.CommandLineException;

public class ParserException extends CommandLineException {
	private static final long serialVersionUID = 1L;

	public ParserException() {
		super();
	}

	public ParserException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ParserException(String message, Throwable cause) {
		super(message, cause);
	}

	public ParserException(String message) {
		super(message);
	}

	public ParserException(Throwable cause) {
		super(cause);
	}
}