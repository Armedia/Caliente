package com.armedia.caliente.cli.parser.token;

import com.armedia.caliente.cli.token.TokenSource;

public class TokenSyntaxException extends TokenProcessorException {
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