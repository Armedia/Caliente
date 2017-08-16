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

public class TokenLoader implements Iterable<Token> {

	private static final List<String> NONE = Collections.emptyList();

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

		public Iterator<String> getStrings() throws Exception {
			if (this.iterator == null) {
				this.strings = TokenLoader.this.cache.get(this.source.getKey());
				if (this.strings == null) {
					// Nothing in the cache? Retrieve it and stash it!
					this.strings = this.source.getTokens();
					TokenLoader.this.cache.put(this.source.getKey(), this.strings);
				}
				if (this.strings == null) {
					this.strings = TokenLoader.NONE;
				}
				this.iterator = this.strings.iterator();
			}
			return this.iterator;
		}
	}

	private Map<String, List<String>> cache = new LinkedHashMap<>();
	private Set<String> recursions = new LinkedHashSet<>();
	private Stack<State> states = new Stack<>();
	private Token next = null;

	private static final String TERMINATOR_FMT = "%s%s";
	private static final String SHORT_FMT = "^%s(\\S)$";
	private static final String LONG_FMT = "^%s%s(\\S+)$";

	private static final Pattern COMMENT = Pattern.compile("(?<!\\\\)#");

	public static final char DEFAULT_PARAMETER_MARKER = '-';
	public static final Character DEFAULT_FILE_MARKER = '@';
	public static final Character DEFAULT_VALUE_SPLITTER = ',';

	private final char parameterMarker;
	private final String terminator;
	private final Pattern patShort;
	private final Pattern patLong;
	private final Character fileMarkerChar;
	private final String fileMarker;
	private final Character valueSeparator;

	public TokenLoader(TokenSource start) {
		this(start, TokenLoader.DEFAULT_PARAMETER_MARKER, TokenLoader.DEFAULT_FILE_MARKER,
			TokenLoader.DEFAULT_VALUE_SPLITTER);
	}

	private TokenLoader(TokenSource start, char parameterMarker, Character fileMarker, Character valueSeparator) {
		if (start == null) { throw new IllegalArgumentException("Must provide a starting token source"); }
		this.states.push(new State(start));
		this.recursions.add(start.getKey());
		this.parameterMarker = parameterMarker;
		this.fileMarkerChar = fileMarker;
		if ((fileMarker != null) && (parameterMarker == fileMarker.charValue())) { throw new IllegalArgumentException(
			"Must provide different characters for paramter marker and file marker"); }
		this.valueSeparator = valueSeparator;
		if ((valueSeparator != null)
			&& (parameterMarker == valueSeparator.charValue())) { throw new IllegalArgumentException(
				"Must provide different characters for paramter marker and value separator"); }

		this.terminator = String.format(TokenLoader.TERMINATOR_FMT, parameterMarker, parameterMarker);
		this.patShort = Pattern.compile(String.format(TokenLoader.SHORT_FMT, parameterMarker));
		this.patLong = Pattern.compile(String.format(TokenLoader.LONG_FMT, parameterMarker, parameterMarker));
		if (fileMarker != null) {
			this.fileMarker = String.format("--%s", fileMarker);
		} else {
			this.fileMarker = null;
		}
	}

	public final char getParameterMarker() {
		return this.parameterMarker;
	}

	public final Character getFileMarker() {
		return this.fileMarkerChar;
	}

	public final Character getValueSplitter() {
		return this.valueSeparator;
	}

	public boolean hasNext() throws Exception {
		// If we have a token waiting in the wings, we go with that...
		if (this.next != null) { return true; }

		// We don't? Ok...then get the next string from our current state,
		// and parse it into a token
		while (!this.states.isEmpty()) {
			final boolean atRoot = (this.states.size() == 1);
			State state = this.states.peek();
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
				this.next = newToken(state, Type.SHORT_OPTION, m.group(1), current);
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

	private State recurse(String current, TokenSource source) throws Exception {
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

	public Token next() throws Exception {
		if (!hasNext()) { throw new NoSuchElementException(); }
		Token ret = this.next;
		this.next = null;
		return ret;
	}

	@Override
	public Iterator<Token> iterator() {
		return new Iterator<Token>() {

			@Override
			public boolean hasNext() {
				try {
					return TokenLoader.this.hasNext();
				} catch (Exception e) {
					throw new RuntimeException("Failed to locate the next token", e);
				}
			}

			@Override
			public Token next() {
				try {
					return TokenLoader.this.next();
				} catch (Exception e) {
					throw new RuntimeException("Failed to load the next token", e);
				}
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

		};
	}
}