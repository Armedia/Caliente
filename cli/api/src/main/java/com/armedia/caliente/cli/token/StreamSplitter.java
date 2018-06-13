package com.armedia.caliente.cli.token;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class StreamSplitter {

	private static Character readNext(Reader r) throws IOException {
		char[] buf = new char[1];
		int read = r.read(buf);
		if (read < 1) { return null; }
		return buf[0];
	}

	private static String resolveEscaped(char c, boolean supportSpaces, Reader in) throws IOException {
		switch (c) {
			case '"':
				return "\"";

			case '\'':
				return "'";

			case ' ':
				if (supportSpaces) { return " "; }
				break;

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
	protected static String readQuoted(Reader r, final char endQuote) throws IOException {
		StringBuilder b = new StringBuilder();
		boolean escaped = false;
		nextChar: while (true) {
			Character current = StreamSplitter.readNext(r);
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
					b.append(StreamSplitter.resolveEscaped(current, false, r));
					continue nextChar;
				}
			}

			b.append(current);
		}
	}

	public static List<String> tokenize(Reader r) throws IOException {
		List<String> ret = new ArrayList<>();

		StringBuilder b = new StringBuilder();
		boolean tokenOpen = false;
		boolean escaped = false;
		nextChar: while (true) {
			Character current = StreamSplitter.readNext(r);
			if (current == null) { return ret; }

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
				b.append(StreamSplitter.resolveEscaped(current, true, r));
				continue nextChar;
			} else {
				selector: switch (current) {
					case ' ':
					case '\t':
					case '\r':
					case '\n':
					case '\f':
						if (!escaped) {
							if (tokenOpen) {
								ret.add(b.toString());
								b.setLength(0);
								tokenOpen = false;
							}
							continue nextChar;
						}
						break selector;

					case '"':
					case '\'':
						if (!escaped) {
							ret.add(StreamSplitter.readQuoted(r, current));
							continue nextChar;
						}
						tokenOpen = true;
						break selector;

					default:
						tokenOpen = true;
						break selector;
				}
			}
			b.append(current);
		}
	}

	public static List<String> tokenize(String str) {
		try {
			return StreamSplitter.tokenize(new StringReader(str));
		} catch (IOException e) {
			throw new RuntimeException("Unexpected IOException while reading from memory", e);
		}
	}
}