package com.armedia.caliente.cli.parser.token;

import com.armedia.caliente.cli.token.Token;
import com.armedia.caliente.cli.token.TokenSource;

public class UnknownSubCommandException extends TokenSyntaxException {
	private static final long serialVersionUID = 1L;

	private final String string;

	public UnknownSubCommandException(Token token) {
		this(token.getSource(), token.getIndex(), token.getRawString());
	}

	public UnknownSubCommandException(TokenSource source, int index, String string) {
		super(source, index);
		this.string = string;
	}

	public final String getString() {
		return this.string;
	}
}