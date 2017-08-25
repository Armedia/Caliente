package com.armedia.caliente.cli;

import com.armedia.caliente.cli.token.Token;
import com.armedia.caliente.cli.token.TokenSource;

public class UnknownCommandException extends CommandLineSyntaxException {
	private static final long serialVersionUID = 1L;

	private final String string;

	public UnknownCommandException(Token token) {
		this(token.getSource(), token.getIndex(), token.getRawString());
	}

	public UnknownCommandException(TokenSource source, int index, String string) {
		super(source, index);
		this.string = string;
	}

	public final String getString() {
		return this.string;
	}
}