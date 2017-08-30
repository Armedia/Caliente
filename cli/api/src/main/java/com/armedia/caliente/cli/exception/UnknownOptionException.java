package com.armedia.caliente.cli.exception;

import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.token.Token;

public class UnknownOptionException extends CommandLineSyntaxException {
	private static final long serialVersionUID = 1L;

	public UnknownOptionException(OptionScheme scheme, Token token) {
		super(scheme, null, token);
	}
}