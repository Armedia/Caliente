package com.armedia.caliente.cli.token;

public class TokenSourceRecursionMissingException extends TokenLoaderException {
	private static final long serialVersionUID = 1L;

	private final TokenSource loopedSource;

	public TokenSourceRecursionMissingException(TokenSource loopedSource) {
		super(String.format("Token source recursion from [%s] is missing the required URL or file parameter",
			loopedSource.getKey()));
		this.loopedSource = loopedSource;
	}

	public final TokenSource getLoopedURL() {
		return this.loopedSource;
	}
}