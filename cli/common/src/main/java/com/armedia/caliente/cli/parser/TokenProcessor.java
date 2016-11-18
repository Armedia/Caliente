package com.armedia.caliente.cli.parser;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.cli.parser.Token.Type;
import com.armedia.commons.utilities.Tools;

public class TokenProcessor {

	private static class TokenCatalog {
		private final List<Token> tokens;
		private final int lastParameter;

		private TokenCatalog(List<Token> tokens) {
			int last = -1;
			loop: for (int i = tokens.size() - 1; i >= 0; i--) {
				Token t = tokens.get(i);
				switch (t.type) {
					case LONG_OPTION:
					case SHORT_OPTION:
						last = i;
						break loop;

					default:
						last *= 1;
						continue;
				}
			}
			this.lastParameter = last;
			this.tokens = Tools.freezeList(tokens);
		}

		@Override
		public String toString() {
			return String.format("TokenCatalog [lastParameter=%d, tokens=%s]", this.lastParameter, this.tokens);
		}
	}

	private static final String TERMINATOR_FMT = "%s%s";
	private static final String SHORT_FMT = "^%s(\\S)$";
	private static final String LONG_FMT = "^%s%s(\\S+)$";

	private static final Pattern FILE_COMMENT = Pattern.compile("(?<!\\\\)#");

	public static final char DEFAULT_PARAMETER_MARKER = '-';
	public static final Character DEFAULT_FILE_MARKER = '@';
	public static final Character DEFAULT_VALUE_SPLITTER = ',';

	private final char parameterMarker;
	private final String terminator;
	private final Pattern patShort;
	private final Pattern patLong;
	private final Character fileMarker;
	private final Character valueSeparator;

	public TokenProcessor() {
		this(TokenProcessor.DEFAULT_PARAMETER_MARKER, TokenProcessor.DEFAULT_FILE_MARKER,
			TokenProcessor.DEFAULT_VALUE_SPLITTER);
	}

	// TODO: Shall we expose these constructurs? What would be the point?
	/*
	private TokenProcessor(char parameterMarker) {
		this(parameterMarker, TokenProcessor.DEFAULT_FILE_MARKER, TokenProcessor.DEFAULT_VALUE_SPLITTER);
	}
	
	private TokenProcessor(char parameterMarker, Character fileMarker) {
		this(parameterMarker, fileMarker, TokenProcessor.DEFAULT_VALUE_SPLITTER);
	}
	*/

	private TokenProcessor(char parameterMarker, Character fileMarker, Character valueSeparator) {
		this.parameterMarker = parameterMarker;
		this.fileMarker = fileMarker;
		if ((fileMarker != null) && (parameterMarker == fileMarker.charValue())) { throw new IllegalArgumentException(
			"Must provide different characters for paramter marker and file marker"); }
		this.valueSeparator = valueSeparator;
		if ((valueSeparator != null)
			&& (parameterMarker == valueSeparator.charValue())) { throw new IllegalArgumentException(
				"Must provide different characters for paramter marker and value separator"); }

		this.terminator = String.format(TokenProcessor.TERMINATOR_FMT, parameterMarker, parameterMarker);
		this.patShort = Pattern.compile(String.format(TokenProcessor.SHORT_FMT, parameterMarker));
		this.patLong = Pattern.compile(String.format(TokenProcessor.LONG_FMT, parameterMarker, parameterMarker));
	}

	public final char getParameterMarker() {
		return this.parameterMarker;
	}

	public final Character getFileMarker() {
		return this.fileMarker;
	}

	public final Character getValueSplitter() {
		return this.valueSeparator;
	}

	private TokenCatalog catalogTokens(Collection<String> args) throws IOException, ParserFileRecursionLoopException {
		return new TokenCatalog(catalogTokens(null, null, null, args));
	}

	private List<Token> catalogTokens(Set<String> recursionGuard, TokenSource source,
		Map<String, List<Token>> tokenSourceCache, Collection<String> args)
		throws IOException, ParserFileRecursionLoopException {
		if (recursionGuard == null) {
			recursionGuard = new LinkedHashSet<>();
		}
		if (tokenSourceCache == null) {
			tokenSourceCache = new HashMap<>();
		}
		boolean terminated = false;
		Matcher m = null;
		List<Token> tokens = new ArrayList<>();
		int i = (source == null ? -1 : 0);
		for (final String s : args) {
			final String sTrim = s.trim();
			i++;
			if (StringUtils.isEmpty(sTrim) && (source != null)) {
				// If we're inside a token source, we skip empty tokens. We don't do that
				// when processing values in the primary
				continue;
			}

			if (terminated) {
				// If we've found a terminator, all other parameters that follow will be treated
				// as plain values, regardless
				tokens.add(new Token(source, i, Type.PLAIN, s, s));
				continue;
			}

			if (this.terminator.equals(sTrim)) {
				if (source == null) {
					// We only add the terminator token at the top level, since for files
					// we simply consume the rest of the file's tokens as plain tokens, and
					// we don't want to terminate the consumption of anything that might come
					// after
					tokens.add(new Token(source, i, Type.TERMINATOR, sTrim, sTrim));
				}
				terminated = true;
				continue;
			}

			m = this.patShort.matcher(sTrim);
			if (m.matches()) {
				tokens.add(new Token(source, i, Type.SHORT_OPTION, m.group(1), sTrim));
				continue;
			}

			m = this.patLong.matcher(sTrim);
			if (m.matches()) {
				tokens.add(new Token(source, i, Type.LONG_OPTION, m.group(1), sTrim));
				continue;
			}

			// If the first character is the file marker, then treat it as a file
			if ((this.fileMarker != null) && (s.charAt(0) == this.fileMarker.charValue())) {
				// Remove the file marker
				String fileName = s.substring(1);
				final TokenSource newSource;
				if (fileName.charAt(0) == this.fileMarker.charValue()) {
					// It's a URL...
					// TODO: Eventually port this to support Commons-VFS URLs?
					final String uri = fileName.substring(1);
					try {
						newSource = new TokenUriSource(uri);
					} catch (URISyntaxException e) {
						throw new IOException(String.format("Failed to properly process the URI [%s]", uri), e);
					}
				} else {
					// It's a local file... if the current source is another local file,
					// and the given path isn't absolute, take its path to be relative to that one
					Path path = Paths.get(fileName);
					if (!path.isAbsolute() && TokenLocalPathSource.class.isInstance(source)) {
						TokenLocalPathSource pathSource = TokenLocalPathSource.class.cast(source);
						Path relativeTo = pathSource.getSourcePath().getParent();
						path = relativeTo.resolve(path);
					}
					newSource = new TokenLocalPathSource(path);
				}

				// Before we recurse, we check...
				if (!recursionGuard
					.add(newSource.getKey())) { throw new ParserFileRecursionLoopException(source, recursionGuard); }

				try {
					List<Token> subTokens = tokenSourceCache.get(newSource.getKey());
					if (subTokens == null) {
						// Nothing cached... fetch the tokens!
						Collection<String> fileArgs = new ArrayList<>();
						for (String line : newSource.getTokens()) {
							Matcher commentMatcher = TokenProcessor.FILE_COMMENT.matcher(line);
							if (commentMatcher.find()) {
								// Strip everything past the first comment character
								line = line.substring(0, commentMatcher.start());
							}
							// If we have any # characters left, we remove all preceding
							// backslashes, to
							// un-escape them
							line = line.replaceAll("\\\\\\\\#", "#");

							fileArgs.add(line.trim());
						}
						if (!fileArgs.isEmpty()) {
							subTokens = catalogTokens(recursionGuard, newSource, tokenSourceCache, fileArgs);
						}
						subTokens = Tools.freezeList(subTokens, true);
						tokenSourceCache.put(newSource.getKey(), subTokens);
					}
					tokens.addAll(subTokens);
				} finally {
					recursionGuard.remove(newSource.getKey());
				}
			}

			tokens.add(new Token(source, i, Type.PLAIN, s, s));
		}
		return tokens;
	}

	private List<String> splitValues(String str) {
		if (str == null) { return Collections.emptyList(); }
		if (this.valueSeparator == null) { return Collections.singletonList(str); }
		return Arrays.asList(StringUtils.splitPreserveAllTokens(str, this.valueSeparator.charValue()));
	}

	public void processTokens(CommandLineInterface rootParams, TokenListener listener, String... args)
		throws MissingParameterValuesException, UnknownParameterException, TooManyValuesException,
		UnknownSubcommandException, ParserFileRecursionLoopException, IOException {

		final Collection<String> c;
		if (args == null) {
			c = Collections.emptyList();
		} else {
			c = Arrays.asList(args);
		}

		processTokens(rootParams, listener, c);
	}

	private boolean processParameter(TokenListener listener, Token token, Parameter parameter, List<String> values)
		throws MissingParameterValuesException, TooManyValuesException {
		final int minValues = parameter.getMinValueCount();
		final int maxValues = parameter.getMaxValueCount();
		final boolean optional = (minValues <= 0);
		final boolean unlimited = (maxValues < 0);

		if (!optional && (values.size() < minValues)) {
			if (listener.tooManyValues(token, parameter,
				values)) { throw new MissingParameterValuesException(token.source, token.index, parameter, values); }
			return false;
		}
		if (!unlimited && (values.size() > maxValues)) {
			if (listener.tooManyValues(token, parameter,
				values)) { throw new TooManyValuesException(token.source, token.index, parameter, values); }
			// If this isn't an error as per the listener...
			return false;
		}

		listener.namedParameterFound(parameter, values);
		return true;
	}

	public void processTokens(CommandLineInterface rootParams, TokenListener listener, Collection<String> args)
		throws ParserFileRecursionLoopException, IOException, UnknownParameterException, UnknownSubcommandException,
		MissingParameterValuesException, TooManyValuesException {

		final TokenCatalog tokens = catalogTokens(args);

		int index = -1;
		ParameterSet parameterSet = rootParams;

		Token currentParameterToken = null;
		Parameter currentParameter = null;
		List<String> currentValues = new ArrayList<>();
		boolean terminated = false;

		for (final Token currentToken : tokens.tokens) {
			index++;

			// If we've reached the terminator, simply add the remaining values verbatim
			// to the currentArgs list, since we'll eventually report them separately
			if (terminated) {
				currentValues.add(currentToken.value);
				continue;
			}

			switch (currentToken.type) {
				case TERMINATOR:
				case SHORT_OPTION:
				case LONG_OPTION:
					if (currentParameterToken != null) {
						processParameter(listener, currentParameterToken, currentParameter, currentValues);
						currentValues.clear();
						currentParameter = null;
						currentParameterToken = null;
					}

					// If this is a terminator, then we terminate, and that's that
					if (currentToken.type == Token.Type.TERMINATOR) {
						terminated = true;
						listener.terminatorFound(currentToken);
						continue;
					}

					// Ok...so...now we need to set up the next parameter
					try {
						final Parameter nextParameter = (currentToken.type == Token.Type.SHORT_OPTION
							? parameterSet.getParameter(currentToken.value.charAt(0))
							: parameterSet.getParameter(currentToken.value));
						if (nextParameter == null) {
							if (!listener.unknownParameterFound(currentToken)) {
								// The parameter is unknown, but this isn't an error, so we simply
								// move on
								continue;
							}
							throw new UnknownParameterException(currentToken.source, currentToken.index,
								currentToken.rawString);
						}

						currentParameterToken = currentToken;
						currentParameter = nextParameter;
					} finally {
						// Regardless of the outcome, the currentArgs list needs to be cleared
						currentValues.clear();
					}
					continue;

				case PLAIN:
					// Are we processing a parameter?
					if (currentParameter != null) {
						final int maxValues = currentParameter.getMaxValueCount();
						// Can this be considered an argument?
						if (maxValues != 0) {
							// Arguments are allowed...so we apply the multivalue splitter
							currentValues.addAll(splitValues(currentToken.value));
							// We check the size now because if concatenated values are allowed,
							// then
							// the count of attribute
							if ((maxValues > 0) && (currentValues.size() > maxValues)) {
								// There is a limit breach...
								if (listener.tooManyValues(currentParameterToken, currentParameter,
									currentValues)) { throw new TooManyValuesException(currentParameterToken.source,
										currentParameterToken.index, currentParameter, currentValues); }
								// If this isn't to be treated as an error, we simply keep going
							}
							// There is no limit on argument count, or the count of arguments
							// is below the set limit
							continue;
						}

						// This isn't an argument...so...it's either a subcommand, a trailing
						// value, or an out-of-place value
					}

					// This token is not an argument b/c this parameter doesn't support
					// them (or we have no parameter)...so it's either a subcommand, or one of the
					// trailing command-line values...
					final ParameterSet subCommand = rootParams.getSubcommand(currentToken.value);
					if (subCommand == null) {
						// Not a subcommand, so ... is this a trailing argument?
						if (index < tokens.lastParameter) {
							if (listener.orphanedValueFound(currentToken)) { throw new UnknownSubcommandException(
								currentToken.source, currentToken.index, currentToken.rawString); }
							// Orphaned value, but not causing a failure
							continue;
						}

						// We're OK..this is a trailing value...
						currentValues.add(currentToken.value);
						continue;
					}

					if (currentParameter != null) {
						processParameter(listener, currentParameterToken, currentParameter, currentValues);
						currentValues.clear();
						currentParameter = null;
						currentParameterToken = null;
					}

					// This is a subcommand, so we replace the current one with this one
					parameterSet = subCommand;
					listener.subCommandFound(currentToken.rawString);

					continue;
			}
		}

		if (!currentValues.isEmpty()) {
			listener.extraArguments(currentValues);
		}
	}
}