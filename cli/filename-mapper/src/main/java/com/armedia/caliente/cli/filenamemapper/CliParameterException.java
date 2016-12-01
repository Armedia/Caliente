package com.armedia.caliente.cli.filenamemapper;

public class CliParameterException extends Exception {
	private static final long serialVersionUID = 1L;

	public CliParameterException() {
	}

	public CliParameterException(String message) {
		super(message);
	}

	public CliParameterException(Throwable cause) {
		super(cause);
	}

	public CliParameterException(String message, Throwable cause) {
		super(message, cause);
	}

	public CliParameterException(String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}