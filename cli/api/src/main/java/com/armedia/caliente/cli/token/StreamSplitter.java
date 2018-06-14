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

	private static String resolveQuotedEscaped(final char c, Reader in) throws IOException {
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

	private static String resolveEscaped(final char c, Reader in) throws IOException {
		switch (c) {
			case ' ':
				return " ";

			case '#':
				return "#";

			default:
				// Invalid sequence... so just replicate it
				break;
		}
		return StreamSplitter.resolveQuotedEscaped(c, in);
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
					b.append(StreamSplitter.resolveQuotedEscaped(current, r));
					continue nextChar;
				}
			}

			b.append(current);
		}
	}

	protected static void readComment(Reader r) throws IOException {
		// Read until the next end-of-line, or end-of-file
		while (true) {
			Character current = StreamSplitter.readNext(r);
			if (current == null) { return; }
			if (current.charValue() == '\n') { return; }
		}
	}

	public static List<String> tokenize(Reader r) throws IOException {
		List<String> ret = new ArrayList<>();

		StringBuilder b = new StringBuilder();
		boolean tokenOpen = false;
		boolean escaped = false;
		nextChar: while (true) {
			Character current = StreamSplitter.readNext(r);
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
				b.append(StreamSplitter.resolveEscaped(current, r));
				continue nextChar;
			} else {
				selector: switch (current) {
					case '#':
						if (tokenOpen) {
							ret.add(b.toString());
							b.setLength(0);
							tokenOpen = false;
						}
						StreamSplitter.readComment(r);
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
						String quoted = StreamSplitter.readQuoted(r, current);
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

	public static List<String> tokenize(String str) {
		try {
			return StreamSplitter.tokenize(new StringReader(str));
		} catch (IOException e) {
			throw new RuntimeException("Unexpected IOException while reading from memory", e);
		}
	}
}