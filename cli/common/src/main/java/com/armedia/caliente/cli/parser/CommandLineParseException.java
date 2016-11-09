package com.armedia.caliente.cli.parser;

import com.armedia.caliente.cli.CommandLineException;

public class CommandLineParseException extends CommandLineException {
	private static final long serialVersionUID = 1L;

	private final String help;

	public CommandLineParseException(String message, Throwable cause, String help) {
		super(message, cause);
		this.help = help;
	}

	public final String getHelp() {
		return this.help;
	}
}