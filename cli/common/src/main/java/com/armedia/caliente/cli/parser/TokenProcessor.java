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

	public List<Token> identifyTokens(String... args) throws IOException, TokenSourceRecursionLoopException {
		if (args == null) { return Collections.emptyList(); }
		return identifyTokens(Arrays.asList(args));
	}

	public List<Token> identifyTokens(Collection<String> args) throws IOException, TokenSourceRecursionLoopException {
		return identifyTokens(null, null, null, args);
	}

	private List<Token> identifyTokens(Set<String> recursionGuard, TokenSource source,
		Map<String, List<Token>> tokenSourceCache, Collection<String> args)
		throws IOException, TokenSourceRecursionLoopException {
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
		nextToken: for (final String rawArg : args) {
			final String current = (source == null ? rawArg : rawArg.trim());
			i++;
			if (StringUtils.isEmpty(current) && (source != null)) {
				// If we're inside a token source, we skip empty tokens. We don't do that
				// when processing values in the primary
				continue;
			}

			// If the first character is the file marker, then treat it as a file
			if ((this.fileMarker != null) && (current.charAt(0) == this.fileMarker.charValue())) {
				// Remove the file marker
				String fileName = current.substring(1);
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
					.add(newSource.getKey())) { throw new TokenSourceRecursionLoopException(source, recursionGuard); }

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
							// backslashes, to un-escape them
							fileArgs.add(line.replaceAll("\\\\#", "#").trim());
						}
						if (!fileArgs.isEmpty()) {
							subTokens = identifyTokens(recursionGuard, newSource, tokenSourceCache, fileArgs);
						}
						subTokens = Tools.freezeList(subTokens, true);
						tokenSourceCache.put(newSource.getKey(), subTokens);
					}
					tokens.addAll(subTokens);
				} finally {
					recursionGuard.remove(newSource.getKey());
				}

				continue nextToken;
			}

			if (terminated) {
				// If we've found a terminator, all other parameters that follow will be treated
				// as plain values, regardless
				tokens.add(new Token(source, i, Type.STRING, current, current));
				continue nextToken;
			}

			if (this.terminator.equals(current)) {
				terminated = true;
				continue nextToken;
			}

			m = this.patShort.matcher(current);
			if (m.matches()) {
				tokens.add(new Token(source, i, Type.SHORT_OPTION, m.group(1), current));
				continue nextToken;
			}

			m = this.patLong.matcher(current);
			if (m.matches()) {
				tokens.add(new Token(source, i, Type.LONG_OPTION, m.group(1), current));
				continue nextToken;
			}

			tokens.add(new Token(source, i, Type.STRING, current, current));
		}
		return tokens;
	}

	private List<String> splitValues(String str) {
		if (str == null) { return Collections.emptyList(); }
		if (this.valueSeparator == null) { return Collections.singletonList(str); }
		return Arrays.asList(StringUtils.splitPreserveAllTokens(str, this.valueSeparator.charValue()));
	}

	public void processTokens(CommandLineInterface rootParams, TokenListener listener, String... args)
		throws MissingParameterValuesException, UnknownParameterException, TooManyParameterValuesException,
		UnknownSubcommandException, TokenSourceRecursionLoopException, IOException {

		final Collection<String> c;
		if (args == null) {
			c = Collections.emptyList();
		} else {
			c = Arrays.asList(args);
		}

		processTokens(rootParams, listener, c);
	}

	private boolean processParameter(TokenListener listener, Token token, Parameter parameter, List<String> values)
		throws MissingParameterValuesException, TooManyParameterValuesException {
		final int minValues = parameter.getMinValueCount();
		final int maxValues = parameter.getMaxValueCount();
		final boolean optional = (minValues <= 0);
		final boolean unlimited = (maxValues < 0);

		if (!optional && (values.size() < minValues)) {
			if (listener.isErrorMissingValues(token, parameter,
				values)) { throw new MissingParameterValuesException(token.source, token.index, parameter, values); }
			return false;
		}

		if (!unlimited && (values.size() > maxValues)) {
			if (listener.isErrorTooManyValues(token, parameter,
				values)) { throw new TooManyParameterValuesException(token.source, token.index, parameter, values); }
			// If this isn't an error as per the listener...
			return false;
		}

		listener.namedParameterFound(parameter, values);
		return true;
	}

	public void processTokens(CommandLineInterface rootParams, TokenListener listener, Collection<String> args)
		throws TokenSourceRecursionLoopException, IOException, UnknownParameterException, UnknownSubcommandException,
		MissingParameterValuesException, TooManyParameterValuesException {

		final List<Token> tokens = identifyTokens(args);

		ParameterSet parameterSet = rootParams;

		Token currentParameterToken = null;
		Parameter currentParameter = null;
		List<String> positionalValues = new ArrayList<>();
		List<String> noValues = Collections.emptyList();
		boolean terminated = false;

		for (final Token currentToken : tokens) {
			switch (currentToken.type) {
				case SHORT_OPTION:
				case LONG_OPTION:
					if (currentParameterToken != null) {
						processParameter(listener, currentParameterToken, currentParameter, noValues);
						currentParameter = null;
						currentParameterToken = null;
					}

					// The erminator only disables processing of --xxx and -x parameters
					if (terminated) {
						positionalValues.add(currentToken.value);
						continue;
					}

					// Ok...so...now we need to set up the next parameter
					final Parameter nextParameter = (currentToken.type == Token.Type.SHORT_OPTION
						? parameterSet.getParameter(currentToken.value.charAt(0))
						: parameterSet.getParameter(currentToken.value));
					if (nextParameter == null) {
						if (!listener.isErrorUnknownParameterFound(currentToken)) {
							// The parameter is unknown, but this isn't an error, so we simply
							// move on
							continue;
						}
						throw new UnknownParameterException(currentToken.source, currentToken.index,
							currentToken.rawString);
					}

					currentParameterToken = currentToken;
					currentParameter = nextParameter;
					continue;

				case STRING:
					// Are we processing a parameter?
					if (currentParameter != null) {
						boolean tokenConsumed = false;
						// Can this be considered an argument?
						List<String> values = noValues;
						if (currentParameter.getMaxValueCount() != 0) {
							// Arguments are allowed...so...submit the argument's value(s)
							values = splitValues(currentToken.value);
							tokenConsumed = true;
						}
						processParameter(listener, currentParameterToken, currentParameter, values);
						currentParameter = null;
						currentParameterToken = null;
						if (tokenConsumed) {
							// The token was consumed and thus shouldn't be processed further...
							continue;
						}
					}

					// This token is not an argument b/c this parameter doesn't support
					// them (or we have no parameter)...so it's either a subcommand, or a
					// positional value
					final ParameterSet subCommand = rootParams.getSubcommand(currentToken.value);
					if (subCommand == null) {
						// We're OK..this is a trailing value...
						positionalValues.add(currentToken.value);
						continue;
					}

					listener.positionalParametersFound(positionalValues);

					// This is a subcommand, so we replace the current one with this one
					parameterSet = subCommand;
					listener.subCommandFound(currentToken.rawString);

					continue;
			}
		}

		if (!positionalValues.isEmpty()) {
			listener.extraArguments(positionalValues);
		}
	}
}