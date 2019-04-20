package com.armedia.caliente.cli.exception;

import com.armedia.caliente.cli.Command;
import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.token.Token;
import com.armedia.commons.utilities.Tools;

public class UnknownOptionException extends CommandLineSyntaxException {
	private static final long serialVersionUID = 1L;

	public UnknownOptionException(OptionScheme scheme, Token token) {
		super(scheme, null, token);
	}

	@Override
	protected String renderMessage() {
		String commandPart = "";
		Command command = Tools.cast(Command.class, getOptionScheme());
		if (command != null) {
			commandPart = String.format(" as part of the '%s' command", command.getName());
		}
		return String.format("The option [%s] is not recognized%s", getToken().getRawString(), commandPart);
	}
}