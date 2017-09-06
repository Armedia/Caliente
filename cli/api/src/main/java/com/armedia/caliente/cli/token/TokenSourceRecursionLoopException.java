package com.armedia.caliente.cli.token;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.armedia.commons.utilities.Tools;

public class TokenSourceRecursionLoopException extends TokenLoaderException {
	private static final long serialVersionUID = 1L;

	private final TokenSource loopedSource;
	private final List<String> sources;

	public TokenSourceRecursionLoopException(TokenSource loopedSource, Collection<String> sources) {
		super(String.format("Token source recursion loop: [%s] is already visited as per %s", loopedSource.getKey(),
			sources));
		this.loopedSource = loopedSource;
		this.sources = Tools.freezeList(new ArrayList<>(sources));
	}

	public final TokenSource getLoopedURL() {
		return this.loopedSource;
	}

	public final List<String> getSources() {
		return this.sources;
	}
}