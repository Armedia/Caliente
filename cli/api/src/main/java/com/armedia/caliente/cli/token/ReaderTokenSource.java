/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
 * %%
 * This file is part of the Caliente software. 
 *  
 * If the software was purchased under a paid Caliente license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *   
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.cli.token;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import com.armedia.commons.utilities.Tools;

public abstract class ReaderTokenSource implements TokenSource {

	public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

	private Throwable thrown = null;
	private List<String> tokenStrings = null;
	private Charset charset = ReaderTokenSource.DEFAULT_CHARSET;

	public synchronized Charset getCharset() {
		return this.charset;
	}

	public synchronized void setCharset(Charset charset) {
		this.charset = Tools.coalesce(this.charset, ReaderTokenSource.DEFAULT_CHARSET);
	}

	protected abstract Reader openReader() throws IOException;

	protected final Character readNext(Reader r) throws IOException {
		char[] buf = new char[1];
		int read = r.read(buf);
		if (read < 1) { return null; }
		return buf[0];
	}

	protected String resolveQuotedEscaped(final char c, Reader in) throws IOException {
		switch (c) {
			case '"':
				return "\"";

			case '\'':
				return "'";

			case 'r':
				return "\r";

			case 'f':
				return "\f";

			case 'n':
				return "\n";

			case 't':
				return "\t";

			case '0':
				// TODO: Should this cause an explosion?
				return "\0";

			case '\\':
				return "\\\\";

			default:
				// Invalid sequence... so just replicate it
				break;
		}
		return String.format("\\%s", c);
	}

	protected String resolveEscaped(final char c, Reader in) throws IOException {
		switch (c) {
			case ' ':
				return " ";

			case '#':
				return "#";

			default:
				// Invalid sequence... so just replicate it
				break;
		}
		return resolveQuotedEscaped(c, in);
	}

	/**
	 * Reads from the reader, concatenating lines (using \n) until the next unescaped instance of
	 * {@code quote} is found. If the end-of-stream is reached before then, an
	 * {@link IllegalStateException} is raised. Escaped (i.e. preceded by \) quote characters are
	 * ignored.
	 *
	 * @param r
	 * @param endQuote
	 * @return the fully-concatenated token, minus the enclosing quote characters
	 * @throws IOException
	 *             forwarded from the reader
	 */
	protected final String readQuoted(Reader r, final char endQuote) throws IOException {
		StringBuilder b = new StringBuilder();
		boolean escaped = false;
		nextChar: while (true) {
			Character current = readNext(r);
			if (current == null) { return b.toString(); }

			if (current == endQuote) {
				if (!escaped) { return b.toString(); }
				escaped = false;
			} else {
				if (current == '\\') {
					if (!escaped) {
						escaped = true;
						continue nextChar;
					}

					// Treat it as a regular character and append it
					escaped = false;
				} else if (escaped) {
					escaped = false;
					b.append(resolveQuotedEscaped(current, r));
					continue nextChar;
				}
			}

			b.append(current);
		}
	}

	protected final void readComment(Reader r) throws IOException {
		// Read until the next end-of-line, or end-of-file
		while (true) {
			Character current = readNext(r);
			if (current == null) { return; }
			if (current.charValue() == '\n') { return; }
		}
	}

	protected List<String> tokenize(Reader r) throws IOException {
		List<String> ret = new ArrayList<>();

		StringBuilder b = new StringBuilder();
		boolean tokenOpen = false;
		boolean escaped = false;
		nextChar: while (true) {
			Character current = readNext(r);
			if (current == null) {
				if (tokenOpen) {
					ret.add(b.toString());
					tokenOpen = false;
				}
				return ret;
			}

			if (current == '\\') {
				if (!escaped) {
					escaped = true;
					continue nextChar;
				}

				// Treat it as a regular character and append it
				escaped = false;
			} else if (escaped) {
				escaped = false;
				tokenOpen = true;
				b.append(resolveEscaped(current, r));
				continue nextChar;
			} else {
				selector: switch (current) {
					case '#':
						if (tokenOpen) {
							ret.add(b.toString());
							b.setLength(0);
							tokenOpen = false;
						}
						readComment(r);
						continue nextChar;

					case ' ':
					case '\t':
					case '\r':
					case '\n':
					case '\f':
						if (tokenOpen) {
							ret.add(b.toString());
							b.setLength(0);
							tokenOpen = false;
						}
						continue nextChar;

					case '"':
					case '\'':
						String quoted = readQuoted(r, current);
						if (tokenOpen) {
							b.append(quoted);
						} else {
							ret.add(quoted);
						}
						continue nextChar;

					default:
						tokenOpen = true;
						break selector;
				}
			}
			b.append(current);
		}
	}

	@Override
	public final synchronized List<String> getTokenStrings() throws IOException {
		if (this.thrown != null) { throw new IOException("An exception has already been raised", this.thrown); }
		if (this.tokenStrings != null) { return this.tokenStrings; }

		// The class is reset, so we perform the read read...
		try (Reader in = openReader()) {
			this.tokenStrings = Tools.freezeList(tokenize(in), true);
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
		this.charset = ReaderTokenSource.DEFAULT_CHARSET;
	}

	@Override
	public abstract String toString();
}