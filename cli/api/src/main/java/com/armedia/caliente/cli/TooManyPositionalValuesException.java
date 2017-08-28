package com.armedia.caliente.cli;

import com.armedia.caliente.cli.token.Token;
import com.armedia.caliente.cli.token.TokenSource;

public class TooManyPositionalValuesException extends CommandLineSyntaxException {
	private static final long serialVersionUID = 1L;

	private final String string;

	public TooManyPositionalValuesException(Token token) {
		this(token.getSource(), token.getIndex(), token.getRawString());
	}

	public TooManyPositionalValuesException(TokenSource source, int index, String string) {
		super(source, index);
		this.string = string;
	}

	public final String getString() {
		return this.string;
	}
}