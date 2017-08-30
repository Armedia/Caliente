package com.armedia.caliente.cli.exception;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.token.Token;

public class CommandLineSyntaxException extends CommandLineException {
	private static final long serialVersionUID = 1L;

	private final OptionScheme optionScheme;
	private final Option option;
	private final Token token;

	protected CommandLineSyntaxException(OptionScheme optionScheme, Option option, Token token) {
		this.optionScheme = optionScheme;
		this.option = option;
		this.token = token;
	}

	public final OptionScheme getOptionScheme() {
		return this.optionScheme;
	}

	public final Option getOption() {
		return this.option;
	}

	public final Token getToken() {
		return this.token;
	}
}