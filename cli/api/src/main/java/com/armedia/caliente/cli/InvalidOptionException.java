package com.armedia.caliente.cli;

public class InvalidOptionException extends CommandLineException {
	private static final long serialVersionUID = 1L;

	public InvalidOptionException(String msg) {
		super(msg);
	}
}