package com.armedia.caliente.cli;

import com.armedia.caliente.cli.token.Token;
import com.armedia.caliente.cli.token.TokenSource;

public class InsufficientPositionalValuesException extends CommandLineSyntaxException {
	private static final long serialVersionUID = 1L;

	private final String string;

	public InsufficientPositionalValuesException(Token token) {
		this(token.getSource(), token.getIndex(), token.getRawString());
	}

	public InsufficientPositionalValuesException(TokenSource source, int index, String string) {
		super(source, index);
		this.string = string;
	}

	public final String getString() {
		return this.string;
	}
}