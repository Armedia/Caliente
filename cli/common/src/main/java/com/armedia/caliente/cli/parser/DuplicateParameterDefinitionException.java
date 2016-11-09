package com.armedia.caliente.cli.parser;

import com.armedia.caliente.cli.CommandLineException;

public class DuplicateParameterDefinitionException extends CommandLineException {
	private static final long serialVersionUID = 1L;

	public DuplicateParameterDefinitionException() {
	}

	public DuplicateParameterDefinitionException(String message) {
		super(message);
	}

	public DuplicateParameterDefinitionException(Throwable cause) {
		super(cause);
	}

	public DuplicateParameterDefinitionException(String message, Throwable cause) {
		super(message, cause);
	}

	public DuplicateParameterDefinitionException(String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}