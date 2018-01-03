package com.armedia.caliente.cli.token;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.armedia.commons.utilities.Tools;

public abstract class StreamTokenSource implements TokenSource {

	public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

	private static final Pattern JOIN_LINE = Pattern.compile("\\\\\\s*$");

	private Throwable thrown = null;
	private List<String> tokenStrings = null;
	private Charset charset = StreamTokenSource.DEFAULT_CHARSET;

	public synchronized Charset getCharset() {
		return this.charset;
	}

	public synchronized void setCharset(Charset charset) {
		this.charset = charset;
	}

	protected abstract InputStream openStream() throws IOException;

	protected List<String> retrieveTokens(Reader r) throws IOException {
		LineNumberReader lr = new LineNumberReader(r);
		List<String> l = new LinkedList<>();
		StringBuilder sb = new StringBuilder();
		boolean continuing = false;
		while (true) {
			String s = lr.readLine();
			if (s == null) {
				break;
			}
			Matcher m = StreamTokenSource.JOIN_LINE.matcher(s);
			if (m.find()) {
				// This line ends with a backslash, so queue it up
				continuing = true;
				sb.append(s.substring(0, m.start()));
				continue;
			}

			// No continuation...
			if (continuing) {
				// If we have accumulated continuations, then finish it off
				// and store the resulting accummulation
				sb.append(s);
				s = sb.toString();
				sb.setLength(0);
				continuing = false;
			}

			l.add(s);
		}
		return l;
	}

	@Override
	public final synchronized List<String> getTokenStrings() throws IOException {
		if (this.thrown != null) { throw new IOException("An exception has already been raised", this.thrown); }
		if (this.tokenStrings != null) { return this.tokenStrings; }

		// The class is reset, so we perform the read read...
		try (InputStream in = openStream()) {
			final Charset charset = Tools.coalesce(getCharset(), StreamTokenSource.DEFAULT_CHARSET);
			this.tokenStrings = Tools.freezeList(retrieveTokens(new InputStreamReader(in, charset)), true);
			this.thrown = null;
		} catch (final IOException e) {
			this.thrown = e;
			this.tokenStrings = null;
			throw e;
		}
		return this.tokenStrings;
	}

	public final synchronized void reset() {
		this.thrown = null;
		this.tokenStrings = null;
		this.charset = StreamTokenSource.DEFAULT_CHARSET;
	}

	@Override
	public abstract String toString();
}