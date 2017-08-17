package com.armedia.caliente.cli.token;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.armedia.commons.utilities.Tools;

public abstract class StreamTokenSource implements TokenSource {

	public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

	private Throwable thrown = null;
	private List<String> parameters = null;
	private Charset charset = StreamTokenSource.DEFAULT_CHARSET;

	public synchronized Charset getCharset() {
		return this.charset;
	}

	public synchronized void setCharset(Charset charset) {
		this.charset = charset;
	}

	protected abstract InputStream openStream() throws IOException;

	protected List<String> retrieveTokens(Reader r) throws IOException {
		return IOUtils.readLines(r);
	}

	@Override
	public final synchronized List<String> getTokenStrings() throws IOException {
		if (this.thrown != null) { throw new IOException("An exception has already been raised", this.thrown); }
		if (this.parameters != null) { return this.parameters; }

		// The class is reset, so we re-read...
		InputStream in = openStream();
		if (in == null) { throw new IOException(String.format("Failed to open the stream from [%s]", getKey())); }
		final Charset charset = Tools.coalesce(getCharset(), StreamTokenSource.DEFAULT_CHARSET);
		try {
			this.parameters = Tools.freezeList(retrieveTokens(new InputStreamReader(in, charset)), true);
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
		this.charset = StreamTokenSource.DEFAULT_CHARSET;
	}
}