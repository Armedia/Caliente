package com.armedia.caliente.cli.exception;

import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.token.Token;

public class TooManyPositionalValuesException extends CommandLineSyntaxException {
	private static final long serialVersionUID = 1L;

	public TooManyPositionalValuesException(OptionScheme scheme, Token token) {
		super(scheme, null, token);
	}
}