package com.armedia.caliente.cli.parser;

import com.armedia.caliente.cli.CommandLineException;

public class HelpRequestedException extends CommandLineException {
	private static final long serialVersionUID = 1L;

	public HelpRequestedException(String help) {
		super(help);
	}
}