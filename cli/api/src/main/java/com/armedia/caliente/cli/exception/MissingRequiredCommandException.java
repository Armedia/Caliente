package com.armedia.caliente.cli.exception;

import com.armedia.caliente.cli.CommandScheme;

public class MissingRequiredCommandException extends CommandLineSyntaxException {
	private static final long serialVersionUID = 1L;

	private final CommandScheme commandScheme;

	public MissingRequiredCommandException(CommandScheme scheme) {
		super(scheme, null, null);
		this.commandScheme = scheme;
	}

	public CommandScheme getCommandScheme() {
		return this.commandScheme;
	}

	@Override
	protected String renderMessage() {
		return "A command name is required";
	}
}