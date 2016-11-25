package com.armedia.caliente.cli.parser.token;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.cli.parser.Parameter;
import com.armedia.caliente.cli.parser.ParameterSchema;
import com.armedia.caliente.cli.parser.ParameterSubSchema;
import com.armedia.caliente.cli.parser.token.Token.Type;
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

	private TokenErrorPolicy defaultErrorPolicy = new BasicParserErrorPolicy();

	public TokenProcessor() {
		this(TokenProcessor.DEFAULT_PARAMETER_MARKER, TokenProcessor.DEFAULT_FILE_MARKER,
			TokenProcessor.DEFAULT_VALUE_SPLITTER);
	}

	// TODO: Shall we expose these constructors? What would be the point?
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

	public TokenErrorPolicy getDefaultErrorPolicy() {
		return this.defaultErrorPolicy;
	}

	public TokenErrorPolicy setDefaultErrorPolicy(TokenErrorPolicy policy) {
		TokenErrorPolicy oldPolicy = this.defaultErrorPolicy;
		if (policy == null) {
			policy = new BasicParserErrorPolicy();
		}
		this.defaultErrorPolicy = policy;
		return oldPolicy;
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

	public List<Token> identifyTokens(Iterable<String> args) throws IOException, TokenSourceRecursionLoopException {
		return identifyTokens(null, null, null, args);
	}

	private List<Token> identifyTokens(Set<String> recursionGuard, TokenSource source,
		Map<String, List<Token>> tokenSourceCache, Iterable<String> args)
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

	public Collection<ParameterData> processTokens(final ParameterSchema rootSet, String... args)
		throws TokenSourceRecursionLoopException, IOException, UnknownParameterException,
		TooManyParameterValuesException, MissingParameterValuesException, UnknownSubCommandException {
		return processTokens(rootSet, null, args);
	}

	public Collection<ParameterData> processTokens(final ParameterSchema rootSet, final TokenErrorPolicy errorPolicy,
		String... args) throws TokenSourceRecursionLoopException, IOException, UnknownParameterException,
		TooManyParameterValuesException, MissingParameterValuesException, UnknownSubCommandException {

		final Collection<String> c;
		if (args == null) {
			c = Collections.emptyList();
		} else {
			c = Arrays.asList(args);
		}

		return processTokens(rootSet, errorPolicy, c);
	}

	public Collection<ParameterData> processTokens(final ParameterSchema rootSet, final Iterable<String> args)
		throws TokenSourceRecursionLoopException, IOException, UnknownParameterException,
		TooManyParameterValuesException, MissingParameterValuesException, UnknownSubCommandException {
		return processTokens(rootSet, null, args);
	}

	private void addNamedValues(ParameterData data, Token token, Parameter parameter, List<String> values,
		TokenErrorPolicy policy) throws TooManyParameterValuesException, MissingParameterValuesException {
		try {
			data.addNamedValues(parameter, values);
		} catch (TooManyParameterValuesException e) {
			if (policy.isErrorTooManyValues(token, parameter, values)) { throw e; }
		} catch (MissingParameterValuesException e) {
			if (policy.isErrorMissingValues(token, parameter, values)) { throw e; }
		}
	}

	public Collection<ParameterData> processTokens(final ParameterSchema rootSet, TokenErrorPolicy errorPolicy,
		final Iterable<String> args) throws TokenSourceRecursionLoopException, IOException, UnknownParameterException,
		TooManyParameterValuesException, MissingParameterValuesException, UnknownSubCommandException {
		if (rootSet == null) { throw new IllegalArgumentException(
			"Must provide a valid ParameterSubSchema to process the tokens against"); }

		final List<Token> tokens = identifyTokens(args);

		Token currentParameterToken = null;
		Parameter currentParameter = null;

		List<String> positionalValues = new ArrayList<>();
		List<String> noValues = Collections.emptyList();

		final Set<String> globalLong = new HashSet<>();
		final Set<Character> globalShort = new HashSet<>();
		for (String p : rootSet.getLongOptions()) {
			globalLong.add(p);
		}
		for (Character p : rootSet.getShortOptions()) {
			globalShort.add(p);
		}

		boolean terminated = false;

		final ParameterData rootData = new DefaultParameterData();

		errorPolicy = Tools.coalesce(errorPolicy, this.defaultErrorPolicy);

		final Collection<ParameterData> data = new LinkedList<>();
		data.add(rootData);

		ParameterSubSchema currentSet = rootSet;
		ParameterData currentData = rootData;
		for (final Token currentToken : tokens) {

			switch (currentToken.type) {
				default:
					// Unhandled token type
					continue;

				case SHORT_OPTION:
				case LONG_OPTION:
					if (currentParameterToken != null) {
						ParameterData target = currentData;
						if (globalLong.contains(currentParameter.getLongOpt())
							|| globalShort.contains(currentParameter.getShortOpt())) {
							target = rootData;
						}
						addNamedValues(target, currentToken, currentParameter, noValues, errorPolicy);
						currentParameter = null;
						currentParameterToken = null;
					}

					// The terminator only disables processing of --xxx and -x parameters
					if (terminated) {
						positionalValues.add(currentToken.value);
						continue;
					}

					// Ok...so...now we need to set up the next parameter
					final Parameter nextParameter = (currentToken.type == Token.Type.SHORT_OPTION
						? currentSet.getParameter(currentToken.value.charAt(0))
						: currentSet.getParameter(currentToken.value));
					if (nextParameter == null) {
						if (!errorPolicy.isErrorUnknownParameterFound(currentToken)) {
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
						ParameterData target = currentData;
						if (globalLong.contains(currentParameter.getLongOpt())
							|| globalShort.contains(currentParameter.getShortOpt())) {
							target = rootData;
						}
						addNamedValues(target, currentToken, currentParameter, values, errorPolicy);
						currentParameter = null;
						currentParameterToken = null;
						if (tokenConsumed) {
							// The token was consumed and thus shouldn't be processed further...
							continue;
						}
					}

					String subName = null;
					ParameterSubSchema subCommand = null;
					if (positionalValues.isEmpty()) {
						// Only the first positional parameter may be a subcommand
						subName = rootSet.getSubSetName(currentToken.value);
						if (subName != null) {
							subCommand = rootSet.getSubSet(subName);
						}
						if ((subCommand == null) && rootSet.isRequiresSubSet()
							&& errorPolicy.isErrorUnknownSubCommandFound(
								currentToken)) { throw new UnknownSubCommandException(currentToken.source,
									currentToken.index, currentToken.rawString); }
					}

					// This isn't a subcommand, so it's a positional value
					if (subCommand == null) {
						positionalValues.add(currentToken.value);
						continue;
					}

					// We're processing a subcommand...so...close out the previous one
					currentData.setPositionalValues(positionalValues);
					positionalValues.clear();

					// This is a subcommand, so we replace the current one with this one
					currentSet = subCommand;
					currentData = new DefaultParameterData(subName);
					data.add(currentData);
					continue;
			}
		}
		currentData.setPositionalValues(positionalValues);
		return data;
	}
}