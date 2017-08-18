package com.armedia.caliente.cli.newlauncher;

import com.armedia.caliente.cli.CommandLineException;

public class CommandLineParsingException extends CommandLineException {
	private static final long serialVersionUID = 1L;

	public CommandLineParsingException() {
	}

	public CommandLineParsingException(String message) {
		super(message);
	}

	public CommandLineParsingException(Throwable cause) {
		super(cause);
	}

	public CommandLineParsingException(String message, Throwable cause) {
		super(message, cause);
	}

	public CommandLineParsingException(String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}