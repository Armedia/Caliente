package com.armedia.caliente.cli.exception;

public class CommandLineRuntimeException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public CommandLineRuntimeException() {
	}

	public CommandLineRuntimeException(String message) {
		super(message);
	}

	public CommandLineRuntimeException(Throwable cause) {
		super(cause);
	}

	public CommandLineRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}

	public CommandLineRuntimeException(String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}