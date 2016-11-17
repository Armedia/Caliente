package com.armedia.caliente.cli.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.armedia.commons.utilities.Tools;

public class TokenConstantSource implements TokenSource {

	private static final AtomicLong counter = new AtomicLong(0);

	private final String key;
	private final List<String> tokens;

	public TokenConstantSource() {
		this(null, null);
	}

	public TokenConstantSource(String key) {
		this(key, null);
	}

	public TokenConstantSource(List<String> tokens) {
		this(null, tokens);
	}

	public TokenConstantSource(String key, Collection<String> tokens) {
		if (key == null) {
			key = String.format("(constant-%016X)", TokenConstantSource.counter.getAndIncrement());
		}
		this.key = key;
		this.tokens = Tools.freezeList(new ArrayList<>(tokens), true);
	}

	@Override
	public List<String> getTokens() throws IOException {
		return this.tokens;
	}

	@Override
	public String getKey() {
		return this.key;
	}
}