package com.armedia.caliente.cli.token;

public class InvalidTokenSourceRecursionException extends TokenLoaderException {
	private static final long serialVersionUID = 1L;

	private final String target;

	public InvalidTokenSourceRecursionException(String target) {
		super(String.format("Token source recursion string [%s] is not valid", target));
		this.target = target;
	}

	public final String getTarget() {
		return this.target;
	}
}