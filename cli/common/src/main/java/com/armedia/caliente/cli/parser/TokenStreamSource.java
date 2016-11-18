package com.armedia.caliente.cli.parser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.armedia.commons.utilities.Tools;

public abstract class TokenStreamSource implements TokenSource {

	private Throwable thrown = null;
	private List<String> parameters = null;

	protected Charset getCharset() {
		return Charset.defaultCharset();
	}

	protected abstract InputStream openStream() throws IOException;

	@Override
	public final synchronized List<String> getTokens() throws IOException {
		if (this.thrown != null) { throw new IOException("An exception has already been raised", this.thrown); }
		if (this.parameters != null) { return this.parameters; }

		// The class is reset, so we re-read...
		InputStream in = openStream();
		if (in == null) { throw new IOException(String.format("Failed to open the stream from [%s]", getKey())); }
		try {
			this.parameters = Tools.freezeList(IOUtils.readLines(in, getCharset()), true);
			this.thrown = null;
		} catch (final IOException e) {
			this.thrown = e;
			this.parameters = null;
			throw e;
		} finally {
			IOUtils.closeQuietly(in);
		}
		return this.parameters;
	}

	public final synchronized void reset() {
		this.thrown = null;
		this.parameters = null;
	}
}