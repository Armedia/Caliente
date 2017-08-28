package com.armedia.caliente.cli;

public class InvalidParameterException extends CommandLineException {
	private static final long serialVersionUID = 1L;

	public InvalidParameterException(String msg) {
		super(msg);
	}
}