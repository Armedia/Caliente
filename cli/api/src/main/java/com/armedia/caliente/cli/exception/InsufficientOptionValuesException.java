package com.armedia.caliente.cli.exception;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.token.Token;
import com.armedia.caliente.cli.token.TokenSource;

public class InsufficientOptionValuesException extends CommandLineSyntaxException {
	private static final long serialVersionUID = 1L;

	private final Option option;
	private final String string;

	public InsufficientOptionValuesException(Option option, Token token) {
		this(option, token.getSource(), token.getIndex(), token.getRawString());
	}

	public InsufficientOptionValuesException(Option option, TokenSource source, int index, String string) {
		super(source, index);
		this.string = string;
		this.option = option;
	}

	public final Option getOption() {
		return this.option;
	}

	public final String getString() {
		return this.string;
	}
}