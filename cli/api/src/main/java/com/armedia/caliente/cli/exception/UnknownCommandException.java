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

	@Override
	protected String renderMessage() {
		return String.format("The command [%s] is not recognized, please use one of these: %s",
			getToken().getRawString(), this.commandScheme.getAliases());
	}
}