package com.armedia.caliente.cli.token;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.cli.token.Token.Type;
import com.armedia.commons.utilities.Tools;

/**
 * <p>
 * This class takes care of loading all the tokens that will eventually conform the entire command
 * line. It will appropriately branch out and resolve references to files (--@path) or URLs
 * (--@@url), and load the contents seamlessly.
 * </p>
 * <p>
 * Remote (i.e. files/urls) token sources are loaded with an equivalency of one token per line, and
 * cleaned up by removing in-line comments (ignoring everything after the first non-escaped #),
 * empty lines, and trimming each token. Fetching of remote tokens is deferred until actually
 * required, and these will only be fetched once (i.e. results are cached). Also, care is taken to
 * avoid recursion loops.
 * </p>
 *
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class TokenLoader implements Iterable<Token> {

	private static final List<String> NO_TOKENS = Collections.emptyList();

	public class TokenIterator {

		private class State {
			private final TokenSource source;
			private List<String> strings = null;
			private Iterator<String> iterator = null;
			private int position = 0;
			private boolean terminated = false;

			private State(TokenSource source) {
				this.source = source;
			}

			private List<String> loadStrings() throws IOException {
				if (this.strings == null) {
					// Nothing in the iteratorCache? Retrieve it and iteratorCache it!
					this.strings = TokenLoader.this.globalCache.get(this.source.getKey());
					if (this.strings == null) {
						this.strings = this.source.getTokenStrings();
						if (this.strings == null) {
							// Nothing was returned? Then we iteratorCache an empty token list
							this.strings = TokenLoader.NO_TOKENS;
						}
						TokenLoader.this.globalCache.put(this.source.getKey(), this.strings);
					}
				}
				return this.strings;
			}

			private Iterator<String> getStrings() throws IOException {
				if (this.iterator == null) {
					this.iterator = loadStrings().iterator();
				}
				return this.iterator;
			}
		}

		private final boolean enableRecursion;
		private final Stack<State> states = new Stack<>();
		private final Set<String> recursions = new LinkedHashSet<>();
		private String multiShort = null;
		private Token next = null;

		private TokenIterator(TokenSource start) {
			this(start, true);
		}

		private TokenIterator(TokenSource start, boolean enableRecursion) {
			this.enableRecursion = enableRecursion;
			this.states.push(new State(start));
			this.recursions.add(start.getKey());
		}

		public boolean hasNext() throws IOException, TokenSourceRecursionLoopException,
			TokenSourceRecursionMissingException, InvalidTokenSourceRecursionException {
			// If we have a token waiting in the wings, we go with that...
			if (this.next != null) { return true; }

			// We don't? Ok...then get the next string from our current state,
			// and parse it into a token
			while (!this.states.isEmpty()) {
				final boolean atRoot = (this.states.size() == 1);
				State state = this.states.peek();

				// If we're processing a multishort (-aBcdEfX), process and return the next short
				// option
				this.next = getNextShortToken(state);
				if (this.next != null) { return true; }

				Iterator<String> it = state.getStrings();
				if (!it.hasNext()) {
					// This state is spent... so remove it
					this.recursions.remove(state.source.getKey());
					this.states.pop();
					continue;
				}

				// There are still strings in the current state's list, so we
				// parse them out...
				final String rawArg = Tools.coalesce(it.next(), "");
				final String current = (atRoot ? rawArg : rawArg.trim());
				state.position++;

				if (StringUtils.isEmpty(current) && !atRoot) {
					// If we're inside a recursion, we skip empty tokens. We don't do that
					// when processing values in the primary (i.e. static command line)
					continue;
				}

				if (state.terminated) {
					// The state has been terminated with -- or equivalent, so all tokens that
					// follow
					// are strictly strings for this source
					this.next = newToken(state, Type.STRING, current, current);
					return true;
				}

				// If the next string is a recursion, then we follow the recursion...
				if (isRecursion(current)) {
					// Here we check if recursion is enabled...if it's not, we just skip the
					// recursion code and ignore the flag altogether
					if (this.enableRecursion) {
						if (!it.hasNext()) { throw new TokenSourceRecursionMissingException(state.source); }
						String recursionTarget = it.next();
						if (StringUtils.isEmpty(recursionTarget)) {
							throw new TokenSourceRecursionMissingException(state.source);
						}
						State newState = recurse(recursionTarget, state.source);
						if (!this.recursions.add(newState.source.getKey())) {
							// Recursion loop!! This is an error!
							throw new TokenSourceRecursionLoopException(newState.source, this.recursions);
						}

						// No recursion loop, so we simply push the new state into the stack and
						// repeat the loop
						this.states.push(newState);
					}
					continue;
				}

				if (TokenLoader.this.terminator.equals(current)) {
					state.terminated = true;
					continue;
				}

				Matcher m = TokenLoader.this.patShort.matcher(current);
				if (m.matches()) {
					this.multiShort = m.group(1);
					this.next = getNextShortToken(state);
					return true;
				}

				m = TokenLoader.this.patLong.matcher(current);
				if (m.matches()) {
					this.next = newToken(state, Type.LONG_OPTION, m.group(1), current);
					return true;
				}

				this.next = newToken(state, Type.STRING, current, current);
				return true;
			}

			return false;
		}

		private Token getNextShortToken(State state) {
			if (this.multiShort == null) { return null; }
			char c = this.multiShort.charAt(0);
			Token next = newToken(state, Type.SHORT_OPTION, String.valueOf(c),
				String.format("%s%s", TokenLoader.OPTION_MARKER, c));
			this.multiShort = this.multiShort.substring(1);
			if (StringUtils.isEmpty(this.multiShort)) {
				this.multiShort = null;
			}
			return next;
		}

		private State recurse(String sourceName, TokenSource source)
			throws IOException, InvalidTokenSourceRecursionException {
			TokenSource newSource = null;

			// It's not a path, so it MUST be a URL...
			// TODO: Eventually port UriTokenSource to support Commons-VFS URLs?
			final URI sourceUri;
			try {
				sourceUri = new URI(sourceName);
				if (UriTokenSource.isSupported(sourceUri)) {
					if (!StringUtils.equals("file", sourceUri.getScheme())) {
						// Not a local file, use the URI
						newSource = new UriTokenSource(sourceUri);
					} else {
						// Local file... treat it as such...
						sourceName = new File(sourceUri).getPath();
					}
				}
			} catch (URISyntaxException e) {
				// Not a URI... must be a path
				newSource = null;
			}

			if (newSource == null) {
				// It's a local file... if the current source is another local file,
				// and the given path isn't absolute, take its path to be relative to that one
				Path sourcePath = null;
				try {
					sourcePath = Paths.get(sourceName);
				} catch (Exception e) {
					// Not a URI nor a path!! KABOOM!
					throw new InvalidTokenSourceRecursionException(sourceName);
				}

				if (!sourcePath.isAbsolute() && LocalPathTokenSource.class.isInstance(source)) {
					LocalPathTokenSource currentSource = LocalPathTokenSource.class.cast(source);
					Path relativeTo = currentSource.getSourcePath().getParent();
					sourcePath = relativeTo.resolve(sourcePath);
				}
				newSource = new LocalPathTokenSource(sourcePath.toAbsolutePath().normalize());
			}

			return new State(newSource);
		}

		private Token newToken(State state, Type type, String value, String rawString) {
			return new Token(state.source, state.position, type, value, rawString);
		}

		public Token next() throws IOException, TokenSourceRecursionLoopException, TokenSourceRecursionMissingException,
			InvalidTokenSourceRecursionException {
			if (!hasNext()) { throw new NoSuchElementException(); }
			Token ret = this.next;
			this.next = null;
			return ret;
		}
	}

	private final Map<String, List<String>> globalCache = new ConcurrentHashMap<>();

	private static final String TERMINATOR_FMT = "%1$s%1$s";
	private static final String SHORT_FMT = "^%1$s(\\S)$";
	private static final String LONG_FMT = "^%1$s%1$s(\\S{2,})$";

	public static final Character DEFAULT_VALUE_SEPARATOR = ',';

	private static final char OPTION_MARKER = '-';
	private static final Character FILE_MARKER = '@';

	private final TokenSource start;
	private final String terminator;
	private final Pattern patShort;
	private final Pattern patLong;
	private final Character fileMarkerChar;
	private final String recursionMarker;
	private final Character valueSeparator;

	/**
	 * Construct a default, recursion-capable TokenLoader instance reading tokens from the given
	 * starting source. This is equivalent to invoking
	 * {@link #TokenLoader(TokenSource, char, boolean) TokenLoader(start,
	 * TokenLoader.DEFAULT_VALUE_SEPARATOR, true)}
	 *
	 * @param start
	 *            the starting TokenSource from which to begin processing
	 */
	public TokenLoader(TokenSource start) {
		this(start, TokenLoader.DEFAULT_VALUE_SEPARATOR, true);
	}

	/**
	 * Construct a default TokenLoader with the option to enable or disable token recursion. This is
	 * equivalent to invoking {@link #TokenLoader(TokenSource, char, boolean) TokenLoader(start,
	 * TokenLoader.DEFAULT_VALUE_SEPARATOR, false)}
	 *
	 * @param start
	 *            the starting TokenSource from which to begin processing
	 * @param allowRecursion
	 *            enable or disable token recursion
	 */
	public TokenLoader(TokenSource start, boolean allowRecursion) {
		this(start, TokenLoader.DEFAULT_VALUE_SEPARATOR, allowRecursion);
	}

	/**
	 * Construct a TokenLoader with recursion enabled, but using the given character as an option
	 * value separator instead of the default value from {@link #DEFAULT_VALUE_SEPARATOR}. This is
	 * equivalent to invoking {@link #TokenLoader(TokenSource, char, boolean) TokenLoader(start,
	 * valueSeparator, true)}
	 *
	 * @param start
	 *            the starting TokenSource from which to begin processing
	 * @param valueSeparator
	 *            the character to use when separating arguments passed for options
	 */
	public TokenLoader(TokenSource start, char valueSeparator) {
		this(start, valueSeparator, true);
	}

	public TokenLoader(TokenSource start, char valueSeparator, boolean allowRecursion) {
		if (start == null) { throw new IllegalArgumentException("Must provide a starting token source"); }
		this.start = start;

		if (allowRecursion) {
			this.fileMarkerChar = TokenLoader.FILE_MARKER;
			this.recursionMarker = String.format("--%s", this.fileMarkerChar);
		} else {
			this.fileMarkerChar = null;
			this.recursionMarker = null;
		}

		this.valueSeparator = valueSeparator;

		this.terminator = String.format(TokenLoader.TERMINATOR_FMT, TokenLoader.OPTION_MARKER);
		this.patShort = Pattern.compile(String.format(TokenLoader.SHORT_FMT, TokenLoader.OPTION_MARKER));
		this.patLong = Pattern.compile(String.format(TokenLoader.LONG_FMT, TokenLoader.OPTION_MARKER));
	}

	public final Character getFileMarker() {
		return this.fileMarkerChar;
	}

	public final Character getValueSeparator() {
		return this.valueSeparator;
	}

	private boolean isRecursion(String str) {
		return (str != null) && StringUtils.equals(this.recursionMarker, str);
	}

	public boolean isRecursionEnabled() {
		return (this.recursionMarker != null);
	}

	public Collection<Token> getBaseTokens() {
		return getTokens(false);
	}

	public Collection<Token> getTokens(boolean recursionEnabled) {
		List<Token> ret = new ArrayList<>();
		TokenIterator it = iterator(recursionEnabled);
		try {
			while (it.hasNext()) {
				ret.add(it.next());
			}
			return ret;
		} catch (IOException e) {
			throw new RuntimeException("Unexpected exception reading from memory", e);
		} catch (TokenSourceRecursionLoopException | TokenSourceRecursionMissingException
			| InvalidTokenSourceRecursionException e) {
			throw new RuntimeException("Recursion exception while parsing the parameter stream", e);
		}
	}

	public void clearCache() {
		this.globalCache.clear();
	}

	public TokenIterator iterator(final boolean enableRecursion) {
		return new TokenIterator(TokenLoader.this.start, enableRecursion);
	}

	@Override
	public Iterator<Token> iterator() {
		return new Iterator<Token>() {
			final TokenIterator it = iterator(true);

			@Override
			public boolean hasNext() {
				try {
					return this.it.hasNext();
				} catch (TokenSourceRecursionLoopException e) {
					throw new RuntimeException("Token recursion loop detected", e);
				} catch (TokenSourceRecursionMissingException e) {
					throw new RuntimeException("Missing recursion target detected", e);
				} catch (InvalidTokenSourceRecursionException e) {
					throw new RuntimeException("Invalid recursion syntax detected", e);
				} catch (IOException e) {
					throw new RuntimeException("Failed to follow a token recursion request", e);
				}
			}

			@Override
			public Token next() {
				try {
					return this.it.next();
				} catch (TokenSourceRecursionLoopException e) {
					throw new RuntimeException("Token recursion loop detected", e);
				} catch (TokenSourceRecursionMissingException e) {
					throw new RuntimeException("Missing recursion target detected", e);
				} catch (InvalidTokenSourceRecursionException e) {
					throw new RuntimeException("Invalid recursion syntax detected", e);
				} catch (IOException e) {
					throw new RuntimeException("Failed to follow a token recursion request", e);
				}
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
}