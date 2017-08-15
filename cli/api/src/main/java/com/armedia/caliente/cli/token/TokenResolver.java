package com.armedia.caliente.cli.token;

import java.io.File;
import java.io.IOException;
import java.net.URI;
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

import com.armedia.caliente.cli.token.Token.Type;
import com.armedia.commons.utilities.Tools;

public class TokenResolver {

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
	private final Character fileMarkerChar;
	private final String fileMarker;
	private final Character valueSeparator;

	public TokenResolver() {
		this(TokenResolver.DEFAULT_PARAMETER_MARKER, TokenResolver.DEFAULT_FILE_MARKER,
			TokenResolver.DEFAULT_VALUE_SPLITTER);
	}

	// TODO: Shall we expose these constructors? What would be the point?
	/*
	private TokenResolver(char parameterMarker) {
		this(parameterMarker, TokenResolver.DEFAULT_FILE_MARKER, TokenResolver.DEFAULT_VALUE_SPLITTER);
	}
	
	private TokenResolver(char parameterMarker, Character fileMarker) {
		this(parameterMarker, fileMarker, TokenResolver.DEFAULT_VALUE_SPLITTER);
	}
	*/

	private TokenResolver(char parameterMarker, Character fileMarker, Character valueSeparator) {
		this.parameterMarker = parameterMarker;
		this.fileMarkerChar = fileMarker;
		if ((fileMarker != null) && (parameterMarker == fileMarker.charValue())) { throw new IllegalArgumentException(
			"Must provide different characters for paramter marker and file marker"); }
		this.valueSeparator = valueSeparator;
		if ((valueSeparator != null)
			&& (parameterMarker == valueSeparator.charValue())) { throw new IllegalArgumentException(
				"Must provide different characters for paramter marker and value separator"); }

		this.terminator = String.format(TokenResolver.TERMINATOR_FMT, parameterMarker, parameterMarker);
		this.patShort = Pattern.compile(String.format(TokenResolver.SHORT_FMT, parameterMarker));
		this.patLong = Pattern.compile(String.format(TokenResolver.LONG_FMT, parameterMarker, parameterMarker));
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
		nextToken: for (String rawArg : args) {
			// By default, skip null strings...
			if (rawArg == null) {
				rawArg = StringUtils.EMPTY;
			}

			final String current = (source == null ? rawArg : rawArg.trim());
			i++;
			if (StringUtils.isEmpty(current) && (source != null)) {
				// If we're inside a token source, we skip empty tokens. We don't do that
				// when processing values in the primary
				continue;
			}

			if (terminated) {
				// If we've found a terminator, all other parameters that follow will be treated
				// as plain values, regardless
				tokens.add(new Token(source, i, Type.STRING, current, current));
				continue nextToken;
			}

			// If this is an include marker, treat it as such
			if ((this.fileMarker != null) && current.startsWith(this.fileMarker)) {
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
						newSource = new TokenUriSource(sourceUri);
					} else {
						// Local file... treat it as such...
						fileName = new File(sourceUri).getAbsolutePath();
					}
				}

				if (newSource == null) {
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
							Matcher commentMatcher = TokenResolver.FILE_COMMENT.matcher(line);
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
}