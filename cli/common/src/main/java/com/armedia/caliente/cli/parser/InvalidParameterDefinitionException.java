package com.armedia.caliente.cli.parser;

import com.armedia.caliente.cli.CommandLineException;

public class InvalidParameterDefinitionException extends CommandLineException {
	private static final long serialVersionUID = 1L;

	public InvalidParameterDefinitionException(String msg) {
		super(msg);
	}
}