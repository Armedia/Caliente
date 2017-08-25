package com.armedia.caliente.cli.token;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;
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

	private class State {
		private final TokenSource source;
		private List<String> strings = null;
		private Iterator<String> iterator = null;
		private int position = 0;
		private boolean terminated = false;

		/**
		 * @param source
		 */
		private State(TokenSource source) {
			this.source = source;
		}

		private Iterator<String> getStrings() throws IOException {
			if (this.iterator == null) {
				this.strings = TokenLoader.this.cache.get(this.source.getKey());
				if (this.strings == null) {
					// Nothing in the cache? Retrieve it and cache it!
					this.strings = this.source.getTokenStrings();
					if (this.strings == null) {
						// Nothing was returned? Then we cache an empty token list
						this.strings = TokenLoader.NO_TOKENS;
					}
					TokenLoader.this.cache.put(this.source.getKey(), this.strings);
				}
				this.iterator = this.strings.iterator();
			}
			return this.iterator;
		}

		private void reset() {
			this.strings = null;
			this.iterator = null;
			this.position = 0;
			this.terminated = false;
		}
	}

	private final Map<String, List<String>> cache = new LinkedHashMap<>();
	private final Set<String> recursions = new LinkedHashSet<>();
	private final Stack<State> states = new Stack<>();
	private String multiShort = null;
	private Token next = null;

	private static final String TERMINATOR_FMT = "%1$s%1$s";
	private static final String SHORT_FMT = "^%1$s(\\S+)$";
	private static final String LONG_FMT = "^%1$s%1$s(\\S+)$";

	private static final Pattern COMMENT = Pattern.compile("(?<!\\\\)#");

	public static final Character DEFAULT_VALUE_SEPARATOR = ',';

	private static final char PARAMETER_MARKER = '-';
	private static final Character FILE_MARKER = '@';

	private final String terminator;
	private final Pattern patShort;
	private final Pattern patLong;
	private final Character fileMarkerChar;
	private final String fileMarker;
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
	 * Construct a TokenLoader with recursion enabled, but using the given character as a parameter
	 * value separator instead of the default value from {@link #DEFAULT_VALUE_SEPARATOR}. This is
	 * equivalent to invoking {@link #TokenLoader(TokenSource, char, boolean) TokenLoader(start,
	 * valueSeparator, true)}
	 *
	 * @param start
	 *            the starting TokenSource from which to begin processing
	 * @param valueSeparator
	 *            the character to use when separating arguments passed for parameters
	 */
	public TokenLoader(TokenSource start, char valueSeparator) {
		this(start, valueSeparator, true);
	}

	public TokenLoader(TokenSource start, char valueSeparator, boolean allowRecursion) {
		if (start == null) { throw new IllegalArgumentException("Must provide a starting token source"); }
		this.states.push(new State(start));
		this.recursions.add(start.getKey());

		if (allowRecursion) {
			this.fileMarkerChar = TokenLoader.FILE_MARKER;
			this.fileMarker = String.format("--%s", this.fileMarkerChar);
		} else {
			this.fileMarkerChar = null;
			this.fileMarker = null;
		}

		this.valueSeparator = valueSeparator;

		this.terminator = String.format(TokenLoader.TERMINATOR_FMT, TokenLoader.PARAMETER_MARKER);
		this.patShort = Pattern.compile(String.format(TokenLoader.SHORT_FMT, TokenLoader.PARAMETER_MARKER));
		this.patLong = Pattern.compile(String.format(TokenLoader.LONG_FMT, TokenLoader.PARAMETER_MARKER));
	}

	public final Character getFileMarker() {
		return this.fileMarkerChar;
	}

	public final Character getValueSplitter() {
		return this.valueSeparator;
	}

	private Token getNextShortToken(State state) {
		if (this.multiShort == null) { return null; }
		char c = this.multiShort.charAt(0);
		Token next = newToken(state, Type.SHORT_OPTION, String.valueOf(c),
			String.format("%s%s", TokenLoader.PARAMETER_MARKER, c));
		this.multiShort = this.multiShort.substring(1);
		if (StringUtils.isEmpty(this.multiShort)) {
			this.multiShort = null;
		}
		return next;
	}

	public boolean hasNext() throws IOException, TokenSourceRecursionLoopException {
		// If we have a token waiting in the wings, we go with that...
		if (this.next != null) { return true; }

		// We don't? Ok...then get the next string from our current state,
		// and parse it into a token
		while (!this.states.isEmpty()) {
			final boolean atRoot = (this.states.size() == 1);
			State state = this.states.peek();

			// If we're processing a multishort (-aBcdEfX), process and return the next short option
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
			final String rawArg = getRawValue(it.next(), atRoot);
			final String current = (atRoot ? rawArg : rawArg.trim());
			state.position++;

			if (StringUtils.isEmpty(current) && !atRoot) {
				// If we're inside a recursion, we skip empty tokens. We don't do that
				// when processing values in the primary (i.e. static command line)
				continue;
			}

			if (state.terminated) {
				// The state has been terminated with -- or equivalent, so all tokens that follow
				// are strictly strings for this source
				this.next = newToken(state, Type.STRING, current, current);
				return true;
			}

			// If the next string is a recursion, then we follow the recursion...
			if (isRecursion(current)) {
				State newState = recurse(current, state.source);
				if (!this.recursions.add(newState.source.getKey())) {
					// Recursion loop!! This is an error!
					throw new TokenSourceRecursionLoopException(newState.source, this.recursions);
				}

				// No recursion loop, so we simply push the new state into the stack and repeat the
				// loop
				this.states.push(newState);
				continue;
			}

			if (this.terminator.equals(current)) {
				state.terminated = true;
				continue;
			}

			Matcher m = this.patShort.matcher(current);
			if (m.matches()) {
				this.multiShort = m.group(1);
				this.next = getNextShortToken(state);
				return true;
			}

			m = this.patLong.matcher(current);
			if (m.matches()) {
				this.next = newToken(state, Type.LONG_OPTION, m.group(1), current);
				return true;
			}

			this.next = newToken(state, Type.STRING, current, current);
			return true;
		}

		return false;
	}

	private Token newToken(State state, Type type, String value, String rawString) {
		return new Token(state.source, state.position, type, value, rawString);
	}

	private String getRawValue(String rawArg, boolean atRoot) {
		rawArg = Tools.coalesce(rawArg, StringUtils.EMPTY);
		if (atRoot) { return rawArg; }
		// If we're not on the root, then we need to remove everything to the right of the
		// first unescaped #, and remove the backslashes from it...
		Matcher commentMatcher = TokenLoader.COMMENT.matcher(rawArg);
		if (commentMatcher.find()) {
			// Strip everything past the first comment character
			rawArg = rawArg.substring(0, commentMatcher.start());
		}
		return rawArg.replaceAll("\\\\#", "#");
	}

	private boolean isRecursion(String str) {
		return (this.fileMarker != null) && str.startsWith(this.fileMarker);
	}

	public boolean isRecursing() {
		return (this.fileMarker != null);
	}

	private State recurse(String current, TokenSource source) throws IOException {
		// Remove the file marker
		String fileName = current.substring(this.fileMarker.length());
		TokenSource newSource = null;
		if (fileName.charAt(0) == this.fileMarkerChar.charValue()) {
			// It's a URL...
			// TODO: Eventually port this to support Commons-VFS URLs?
			final String uriStr = fileName.substring(1);
			final URI sourceUri;
			try {
				sourceUri = new URI(uriStr);
			} catch (URISyntaxException e) {
				throw new IOException(String.format("Failed to properly process the URI [%s]", uriStr), e);
			}
			if (!sourceUri.getScheme().equals("file")) {
				// Not a local file, use the URL source
				newSource = new UriTokenSource(sourceUri);
			} else {
				// Local file... treat it as such...
				fileName = new File(sourceUri).getAbsolutePath();
			}
		}

		if (newSource == null) {
			// It's a local file... if the current source is another local file,
			// and the given path isn't absolute, take its path to be relative to that one
			Path path = Paths.get(fileName);
			if (!path.isAbsolute() && LocalPathTokenSource.class.isInstance(source)) {
				LocalPathTokenSource pathSource = LocalPathTokenSource.class.cast(source);
				Path relativeTo = pathSource.getSourcePath().getParent();
				path = relativeTo.resolve(path);
			}
			newSource = new LocalPathTokenSource(path);
		}

		return new State(newSource);
	}

	public Token next() throws IOException, TokenSourceRecursionLoopException {
		if (!hasNext()) { throw new NoSuchElementException(); }
		Token ret = this.next;
		this.next = null;
		return ret;
	}

	public void restart() {
		while (this.states.size() > 1) {
			State s = this.states.pop();
			this.recursions.remove(s.source.getKey());
		}
		this.states.peek().reset();
		this.next = null;
	}

	public void reset() {
		restart();
		this.cache.clear();
	}

	@Override
	public Iterator<Token> iterator() {
		return new Iterator<Token>() {

			@Override
			public boolean hasNext() {
				try {
					return TokenLoader.this.hasNext();
				} catch (TokenSourceRecursionLoopException e) {
					throw new RuntimeException("Token recursion loop detected", e);
				} catch (IOException e) {
					throw new RuntimeException("Failed to follow a token recursion request", e);
				}
			}

			@Override
			public Token next() {
				try {
					return TokenLoader.this.next();
				} catch (TokenSourceRecursionLoopException e) {
					throw new RuntimeException("Token recursion loop detected", e);
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