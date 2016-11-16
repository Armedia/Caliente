package com.armedia.caliente.cli.parser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.Tools;

public class Parser {

	private static enum TokenType {
		//
		/**
		 * A short option (i.e. -c, -x, etc.) its value is always one character long
		 */
		SHORT_OPTION,

		/**
		 * A long option (i.e. --long-option) - its value is always a string
		 */
		LONG_OPTION,

		/**
		 * A "plain" string - i.e. no prefix of any kind
		 */
		PLAIN,

		/**
		 * The special value '--' to terminate the argument list
		 */
		TERMINATOR,
		//
		;
	}

	/**
	 * <p>
	 * An object signifying a token that will be part of the parameter stream. It indicates not only
	 * the {@link TokenType type of the token}, information about where it was read from (the main
	 * parameter stream or a parameter file), as well as its relative position within that sourceStr
	 * (index within the parameter stream, or line number within the parameter file).
	 *
	 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
	 *
	 */
	private static class Token {
		/**
		 * <p>
		 * The file from which the token was read. The value {@code null} means that it was read
		 * from the main parameter stream.
		 * </p>
		 */
		private final File sourceFile;

		/**
		 * <p>
		 * The index from within the sourceStr that the token was read. When {@link #sourceFile} is
		 * {@code null}, this means the index within the parameter stream. Otherwise, it means the
		 * line number within the file.
		 * </p>
		 */
		private final int index;

		/**
		 * <p>
		 * The type of the token
		 * </p>
		 */
		private final TokenType type;

		/**
		 * <p>
		 * The token's value, minus any applicable prefixes.
		 * </p>
		 */
		private final String value;

		/**
		 * <p>
		 * The original string the token was derived from
		 * </p>
		 */
		private final String sourceStr;

		private Token(File sourceFile, int index, TokenType type, String value, String source) {
			this.sourceFile = sourceFile;
			this.index = index;
			this.type = type;
			this.value = value;
			this.sourceStr = source;
		}

		@Override
		public String toString() {
			return String.format("Token [sourceFile=%s, index=%s, type=%s, value=%s, sourceStr=%s]", this.sourceFile,
				this.index, this.type, this.value, this.sourceStr);
		}
	}

	private static class TokenCatalog {
		private final List<Token> tokens;
		private final int lastParameter;

		private TokenCatalog(List<Token> tokens) {
			int last = -1;
			for (int i = tokens.size() - 1; i >= 0; i++) {
				Token t = tokens.get(i);
				switch (t.type) {
					case LONG_OPTION:
					case SHORT_OPTION:
						last = i;
						// fall-through
					case PLAIN:
						continue;

					case TERMINATOR:
						break;
				}
			}
			this.lastParameter = last;
			this.tokens = Tools.freezeList(tokens);
		}
	}

	public static interface ParameterSet {

		public ParameterSet getSub(String name);

		public Parameter getShort(char shortOption);

		public Parameter getLong(String longOption);

	}

	private static final String[] NO_ARGS = {};

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

	public Parser() {
		this(Parser.DEFAULT_PARAMETER_MARKER, Parser.DEFAULT_FILE_MARKER, Parser.DEFAULT_VALUE_SPLITTER);
	}

	public Parser(char parameterMarker) {
		this(parameterMarker, Parser.DEFAULT_FILE_MARKER, Parser.DEFAULT_VALUE_SPLITTER);
	}

	public Parser(char parameterMarker, Character fileMarker) {
		this(parameterMarker, fileMarker, Parser.DEFAULT_VALUE_SPLITTER);
	}

	public Parser(char parameterMarker, Character fileMarker, Character valueSeparator) {
		this.parameterMarker = parameterMarker;
		this.fileMarker = fileMarker;
		if ((fileMarker != null) && (parameterMarker == fileMarker.charValue())) { throw new IllegalArgumentException(
			"Must provide different characters for paramter marker and file marker"); }
		this.valueSeparator = valueSeparator;
		if ((valueSeparator != null)
			&& (parameterMarker == valueSeparator.charValue())) { throw new IllegalArgumentException(
				"Must provide different characters for paramter marker and value separator"); }

		this.terminator = String.format(Parser.TERMINATOR_FMT, parameterMarker, parameterMarker);
		this.patShort = Pattern.compile(String.format(Parser.SHORT_FMT, parameterMarker));
		this.patLong = Pattern.compile(String.format(Parser.LONG_FMT, parameterMarker, parameterMarker));
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

	private TokenCatalog catalogTokens(String... args)
		throws ParserFileAccessException, ParserFileRecursionLoopException {
		return new TokenCatalog(catalogTokens(null, null, args));
	}

	private List<Token> catalogTokens(Set<String> fileRecursion, File sourceFile, String... args)
		throws ParserFileAccessException, ParserFileRecursionLoopException {
		if (fileRecursion == null) {
			fileRecursion = new LinkedHashSet<>();
		}
		if ((sourceFile != null) && !fileRecursion.add(
			sourceFile.getAbsolutePath())) { throw new ParserFileRecursionLoopException(sourceFile, fileRecursion); }
		boolean terminated = false;
		try {
			Matcher m = null;
			List<Token> tokens = new ArrayList<>();
			int i = -1;
			for (final String s : args) {
				final String sTrim = s.trim();
				i++;
				if (StringUtils.isEmpty(sTrim) && (sourceFile != null)) {
					// If we're inside a file, we skip empty tokens. We don't do that
					// when processing values in the primary
					continue;
				}

				if (terminated) {
					tokens.add(new Token(sourceFile, i, TokenType.PLAIN, s, s));
					continue;
				}

				if (this.terminator.equals(sTrim)) {
					if (sourceFile == null) {
						// We only add the terminator token at the top level, since for files
						// we simply consume the rest of the file's tokens as plain tokens, and
						// we don't want to terminate parsing of anything that might come after
						tokens.add(new Token(sourceFile, i, TokenType.TERMINATOR, sTrim, sTrim));
					}
					terminated = true;
					continue;
				}

				m = this.patShort.matcher(sTrim);
				if (m.matches()) {
					tokens.add(new Token(sourceFile, i, TokenType.SHORT_OPTION, m.group(1), sTrim));
					continue;
				}

				m = this.patLong.matcher(sTrim);
				if (m.matches()) {
					tokens.add(new Token(sourceFile, i, TokenType.LONG_OPTION, m.group(1), sTrim));
					continue;
				}

				// If the first non-whitespace character is the file marker, then treat it as a file
				if ((this.fileMarker != null) && (sTrim.charAt(0) == this.fileMarker.charValue())) {
					String fileName = s.substring(s.indexOf(this.fileMarker.charValue()) + 1);
					File parameterFile = new File(fileName);
					try {
						parameterFile = parameterFile.getCanonicalFile();
					} catch (IOException e) {
						// Do nothing...log it, maybe?
					} finally {
						parameterFile = parameterFile.getAbsoluteFile();
					}
					final List<String> lines;
					try {
						lines = FileUtils.readLines(parameterFile, Charset.forName("UTF-8"));
					} catch (IOException e) {
						throw new ParserFileAccessException(parameterFile, e);
					}
					List<String> fileArgs = new ArrayList<>();
					for (String line : lines) {
						Matcher commentMatcher = Parser.FILE_COMMENT.matcher(line);
						if (commentMatcher.find()) {
							// Strip everything past the first comment character
							line = line.substring(0, commentMatcher.start() + 1);
						}
						fileArgs.add(line.trim());
					}
					if (!fileArgs.isEmpty()) {
						List<Token> fileTokens = catalogTokens(fileRecursion, parameterFile,
							fileArgs.toArray(Parser.NO_ARGS));
						if (fileTokens != null) {
							tokens.addAll(fileTokens);
						}
					}
				}

				tokens.add(new Token(sourceFile, i, TokenType.PLAIN, s, s));
			}
			return tokens;
		} finally {
			if (sourceFile != null) {
				// Remove the current file from the recursion loop
				fileRecursion.remove(sourceFile.getAbsolutePath());
			}
		}
	}

	private List<String> splitValues(String str) {
		if (str == null) { return Collections.emptyList(); }
		if (this.valueSeparator == null) { return Collections.singletonList(str); }
		return Arrays.asList(StringUtils.splitPreserveAllTokens(str, this.valueSeparator.charValue()));
	}

	public void parse(ParameterSet params, ParserListener listener, String... args)
		throws MissingParameterValueException, UnknownParameterException, TooManyValuesException,
		UnknownSubcommandException, ParserFileAccessException, ParserFileRecursionLoopException {

		final TokenCatalog tokens = catalogTokens(args);

		int i = -1;
		Token currentParameterToken = null;
		Parameter currentParameter = null;
		List<String> currentArgs = new ArrayList<>();
		boolean terminated = false;
		for (final Token currentToken : tokens.tokens) {
			i++;

			if (terminated) {
				currentArgs.addAll(splitValues(currentToken.value));
				continue;
			}

			switch (currentToken.type) {

				case TERMINATOR:
				case SHORT_OPTION:
				case LONG_OPTION:
					if (currentParameter != null) {
						if (!currentParameter.isValueOptional() && (currentParameter.getValueCount() != 0)
							&& currentArgs.isEmpty()) {
							// The current parameter requires values, so complain loudly, or stow
							// the complaint for later...
							if (listener.missingValues(currentParameter)) { throw new MissingParameterValueException(
								currentParameterToken.sourceFile, currentParameterToken.index, currentParameter); }
							continue;
						}

						// The current parameter is valid, so we close it up and clear the argument
						// list
						listener.parameterFound(currentParameter, currentArgs);
						currentArgs.clear();
						currentParameter = null;
						currentParameterToken = null;
					}

					// If this is a terminator, then we terminate, and that's that
					if (currentToken.type == TokenType.TERMINATOR) {
						terminated = true;
						listener.terminatorFound();
						continue;
					}

					// Ok...so...now we need to set up the next parameter
					try {
						final Parameter nextParameter = (currentToken.type == TokenType.SHORT_OPTION
							? params.getShort(currentToken.value.charAt(0)) : params.getLong(currentToken.value));
						if (nextParameter == null) {
							if (!listener.unknownParameterFound(currentToken.sourceFile, currentToken.index,
								currentToken.sourceStr)) {
								// The parameter is unknown, but this isn't an error, so we simply
								// move on
								continue;
							}
							throw new UnknownParameterException(currentToken.sourceFile, currentToken.index,
								currentToken.sourceStr);
						}

						currentParameterToken = currentToken;
						currentParameter = nextParameter;
					} finally {
						// Regardless of the outcome, the currentArgs list needs to be cleared
						currentArgs.clear();
					}
					continue;

				case PLAIN:
					// Are we processing a parameter?
					if (currentParameter != null) {
						// Can this be considered an argument?
						if (currentParameter.getValueCount() != 0) {
							// Arguments are allowed...
							currentArgs.addAll(splitValues(currentToken.value));
							if ((currentParameter.getValueCount() > 0)
								&& (currentArgs.size() > currentParameter.getValueCount())) {
								// There is a limit breach...
								if (listener.tooManyValues(currentParameter,
									currentArgs)) { throw new TooManyValuesException(currentParameterToken.sourceFile,
										currentParameterToken.index, currentParameter, currentArgs); }
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

					ParameterSet sub = params.getSub(currentToken.value);
					if (sub == null) {
						// Not a subcommand, so ... is this a trailing argument?
						if (i < tokens.lastParameter) {
							if (listener.orphanedValueFound(currentToken.sourceFile, currentToken.index,
								currentToken.value)) { throw new UnknownSubcommandException(currentToken.sourceFile,
									currentToken.index, currentToken.sourceStr); }
							// Orphaned value, but not causing a failure
							continue;
						}

						// We're OK..this is a trailing value...
						currentArgs.addAll(splitValues(currentToken.value));
						continue;
					}

					// This is a subcommand, so we replace the current one with this one
					params = sub;
					listener.subCommandFound(currentToken.sourceStr);
					continue;
			}
		}

		if (!currentArgs.isEmpty()) {
			listener.extraArguments(currentArgs);
		}
	}
}