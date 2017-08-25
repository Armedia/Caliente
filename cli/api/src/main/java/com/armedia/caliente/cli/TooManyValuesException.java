package com.armedia.caliente.cli;

import com.armedia.caliente.cli.token.Token;
import com.armedia.caliente.cli.token.TokenSource;

public class TooManyValuesException extends CommandLineSyntaxException {
	private static final long serialVersionUID = 1L;

	private final String string;

	public TooManyValuesException(Token token) {
		this(token.getSource(), token.getIndex(), token.getRawString());
	}

	public TooManyValuesException(TokenSource source, int index, String string) {
		super(source, index);
		this.string = string;
	}

	public final String getString() {
		return this.string;
	}
}