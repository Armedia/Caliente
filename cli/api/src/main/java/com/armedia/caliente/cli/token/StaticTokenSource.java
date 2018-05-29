package com.armedia.caliente.cli.token;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.armedia.commons.utilities.Tools;

public class StaticTokenSource implements TokenSource {

	private static final AtomicLong counter = new AtomicLong(0);

	private final String key;
	private final List<String> tokens;

	public StaticTokenSource() {
		this(null, null);
	}

	public StaticTokenSource(String key) {
		this(key, null);
	}

	public StaticTokenSource(Collection<String> tokens) {
		this(null, tokens);
	}

	public StaticTokenSource(String key, Collection<String> tokens) {
		if (key == null) {
			key = String.format("(static-%016X)", StaticTokenSource.counter.getAndIncrement());
		}
		this.key = key;
		this.tokens = Tools.freezeList(new ArrayList<>(tokens));
	}

	@Override
	public List<String> getTokenStrings() throws IOException {
		return this.tokens;
	}

	@Override
	public String getKey() {
		return this.key;
	}

	@Override
	public String toString() {
		return String.format("StaticTokenSource [key=%s]", this.key);
	}
}