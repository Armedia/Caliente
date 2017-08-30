package com.armedia.caliente.cli.exception;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.token.Token;

public class TooManyOptionValuesException extends CommandLineSyntaxException {
	private static final long serialVersionUID = 1L;

	public TooManyOptionValuesException(OptionScheme scheme, Option option, Token token) {
		super(scheme, option, token);
	}
}