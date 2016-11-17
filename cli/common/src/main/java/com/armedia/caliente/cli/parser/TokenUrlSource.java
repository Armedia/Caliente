package com.armedia.caliente.cli.parser;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class TokenUrlSource extends TokenStreamSource {

	private final URL sourceUrl;

	public TokenUrlSource(String sourceUrl) throws MalformedURLException {
		if (sourceUrl == null) { throw new IllegalArgumentException("Must provide a non-null String object"); }
		this.sourceUrl = new URL(sourceUrl);
	}

	public TokenUrlSource(URL sourceUrl) {
		if (sourceUrl == null) { throw new IllegalArgumentException("Must provide a non-null URL object"); }
		this.sourceUrl = sourceUrl;
	}

	@Override
	public String getKey() {
		return this.sourceUrl.toString();
	}

	@Override
	protected InputStream openStream() throws IOException {
		return this.sourceUrl.openStream();
	}
}