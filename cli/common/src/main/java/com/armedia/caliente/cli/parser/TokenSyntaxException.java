package com.armedia.caliente.cli.parser;

public class TokenSyntaxException extends ParserException {
	private static final long serialVersionUID = 1L;

	private final TokenSource source;
	private final int index;

	protected TokenSyntaxException(TokenSource source, int index) {
		this.source = source;
		this.index = index;
	}

	public final TokenSource getSource() {
		return this.source;
	}

	public final int getIndex() {
		return this.index;
	}
}