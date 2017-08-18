package com.armedia.caliente.cli.newlauncher;

import com.armedia.caliente.cli.CommandLineException;

public class CommandLineProcessingException extends CommandLineException {
	private static final long serialVersionUID = 1L;

	private final int returnValue;

	public CommandLineProcessingException(int returnValue) {
		this.returnValue = returnValue;
	}

	public CommandLineProcessingException(int returnValue, String message) {
		super(message);
		this.returnValue = returnValue;
	}

	public CommandLineProcessingException(int returnValue, Throwable cause) {
		super(cause);
		this.returnValue = returnValue;
	}

	public CommandLineProcessingException(int returnValue, String message, Throwable cause) {
		super(message, cause);
		this.returnValue = returnValue;
	}

	public CommandLineProcessingException(int returnValue, String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		this.returnValue = returnValue;
	}

	public int getReturnValue() {
		return this.returnValue;
	}

}
