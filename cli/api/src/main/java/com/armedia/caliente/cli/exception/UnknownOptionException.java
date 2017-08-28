package com.armedia.caliente.cli.exception;

import com.armedia.caliente.cli.token.Token;
import com.armedia.caliente.cli.token.TokenSource;

public class UnknownOptionException extends CommandLineSyntaxException {
	private static final long serialVersionUID = 1L;

	private final String string;

	public UnknownOptionException(Token token) {
		this(token.getSource(), token.getIndex(), token.getRawString());
	}

	public UnknownOptionException(TokenSource source, int index, String string) {
		super(source, index);
		this.string = string;
	}

	public final String getString() {
		return this.string;
	}
}
