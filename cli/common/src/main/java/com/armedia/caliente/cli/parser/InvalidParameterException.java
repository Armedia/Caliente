package com.armedia.caliente.cli.parser;

import com.armedia.caliente.cli.CommandLineException;

public class InvalidParameterException extends CommandLineException {
	private static final long serialVersionUID = 1L;

	public InvalidParameterException(String msg) {
		super(msg);
	}
}