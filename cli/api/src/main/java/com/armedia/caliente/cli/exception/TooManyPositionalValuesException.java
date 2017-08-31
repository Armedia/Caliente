package com.armedia.caliente.cli.exception;

import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.token.Token;

public class TooManyPositionalValuesException extends CommandLineSyntaxException {
	private static final long serialVersionUID = 1L;

	public TooManyPositionalValuesException(OptionScheme scheme, Token token) {
		super(scheme, null, token);
	}

	@Override
	protected String renderMessage() {
		int min = getOptionScheme().getMinArgs();
		int max = getOptionScheme().getMaxArgs();

		String msg = "";
		if (min == max) {
			if (max == 0) {
				msg = "no";
			} else {
				msg = String.format("exactly %d", max);
			}
		} else {
			msg = String.format("at most %d", max);
		}

		return String.format("Too many positional values - %s positional arguments are allowed", msg);
	}
}