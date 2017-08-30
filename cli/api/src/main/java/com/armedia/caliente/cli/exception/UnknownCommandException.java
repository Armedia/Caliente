package com.armedia.caliente.cli.exception;

import com.armedia.caliente.cli.CommandScheme;
import com.armedia.caliente.cli.token.Token;

public class UnknownCommandException extends CommandLineSyntaxException {
	private static final long serialVersionUID = 1L;

	private final CommandScheme commandScheme;

	public UnknownCommandException(CommandScheme scheme, Token token) {
		super(scheme, null, token);
		this.commandScheme = scheme;
	}

	public final CommandScheme getCommandScheme() {
		return this.commandScheme;
	}
}