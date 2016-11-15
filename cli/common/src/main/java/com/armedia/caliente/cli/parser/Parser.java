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

	private static final String TERMINATOR = "--";
	private static final Pattern SHORT = Pattern.compile("^-(\\S)$");
	private static final Pattern LONG = Pattern.compile("^--(\\S+)$");
	private static final Pattern FILE = Pattern.compile("^@(.+)$");
	private static final Pattern FILE_COMMENT = Pattern.compile("(?<!\\\\)#");

	private static TokenCatalog catalogTokens(String... args) throws IOException, ParserException {
		return new TokenCatalog(Parser.catalogTokens(null, null, args));
	}

	private static List<Token> catalogTokens(Set<String> fileRecursion, File sourceFile, String... args)
		throws IOException, ParserException {
		if (fileRecursion == null) {
			fileRecursion = new LinkedHashSet<>();
		}
		if ((sourceFile != null) && !fileRecursion.add(sourceFile.getAbsolutePath())) { throw new ParserException(
			String.format("Recursion loop detected: %s", fileRecursion)); }
		boolean terminated = false;
		try {
			Matcher m = null;
			List<Token> tokens = new ArrayList<>();
			int i = -1;
			for (String s : args) {
				i++;
				if (StringUtils.isBlank(s) && (sourceFile != null)) {
					// If we're inside a file, we skip empty tokens. We don't do that
					// when processing values in the primary
					continue;
				}
				if (terminated) {
					tokens.add(new Token(sourceFile, i, TokenType.PLAIN, s, s));
					continue;
				}

				if (Parser.TERMINATOR.equals(s)) {
					if (sourceFile == null) {
						// We only add the terminator token at the top level, since for files
						// we simply consume the rest of the file's tokens as plain tokens, and
						// we don't want to terminate parsing of anything that might come after
						tokens.add(new Token(sourceFile, i, TokenType.TERMINATOR, s, s));
					}
					terminated = true;
					continue;
				}

				m = Parser.SHORT.matcher(s);
				if (m.matches()) {
					tokens.add(new Token(sourceFile, i, TokenType.SHORT_OPTION, m.group(1), s));
					continue;
				}

				m = Parser.LONG.matcher(s);
				if (m.matches()) {
					tokens.add(new Token(sourceFile, i, TokenType.LONG_OPTION, m.group(1), s));
					continue;
				}

				m = Parser.FILE.matcher(s);
				if (m.matches()) {
					File parameterFile = new File(m.group(1));
					try {
						parameterFile = parameterFile.getCanonicalFile();
					} catch (IOException e) {
						// Do nothing...log it, maybe?
					} finally {
						parameterFile = parameterFile.getAbsoluteFile();
					}
					List<String> lines = FileUtils.readLines(parameterFile, Charset.forName("UTF-8"));
					List<String> fileArgs = new ArrayList<>();
					for (String line : lines) {
						Matcher commentMatcher = Parser.FILE_COMMENT.matcher(line);
						if (commentMatcher.find()) {
							// Strip everything past the first comment character
							line = line.substring(0, commentMatcher.start() + 1);
						}
						fileArgs.add(line);
					}
					if (!fileArgs.isEmpty()) {
						List<Token> fileTokens = Parser.catalogTokens(fileRecursion, parameterFile,
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

	private static List<String> split(Character splitChar, String str) {
		if (str == null) { return Collections.emptyList(); }
		if (splitChar == null) { return Collections.singletonList(str); }
		return Arrays.asList(StringUtils.splitPreserveAllTokens(str, splitChar.charValue()));
	}

	public static void parse(ParameterSet params, ParserListener listener, Character multiValueSplit, String... args)
		throws ParserException {

		final TokenCatalog tokens;
		try {
			tokens = Parser.catalogTokens(args);
		} catch (IOException e) {
			throw new ParserException("Failed to load all the required parameters", e);
		}

		int i = -1;
		Parameter current = null;
		List<String> currentArgs = new ArrayList<>();
		boolean terminated = false;
		for (final Token token : tokens.tokens) {
			i++;

			if (terminated) {
				currentArgs.addAll(Parser.split(multiValueSplit, token.value));
				continue;
			}

			switch (token.type) {

				case TERMINATOR:
				case SHORT_OPTION:
				case LONG_OPTION:
					if (current != null) {
						if (!current.isValueOptional() && (current.getValueCount() != 0) && currentArgs.isEmpty()) {
							// The current parameter requires values, so complain loudly, or stow
							// the complaint for later...
							if (listener.errorMissingArguments(current)) { throw new ParserException(); }
							continue;
						}

						// The current parameter is valid, so we close it up and clear the argument
						// list
						listener.parameterFound(current, currentArgs);
						currentArgs.clear();
					}

					// If this is a terminator, then we terminate, and that's that
					if (token.type == TokenType.TERMINATOR) {
						terminated = true;
						listener.terminatorFound();
						continue;
					}

					// Ok...so...now we need to set up the next parameter
					try {
						final Parameter next = (token.type == TokenType.SHORT_OPTION
							? params.getShort(token.value.charAt(0)) : params.getLong(token.value));
						if (next == null) {
							if (!listener.errorUnknownParameter(token.sourceStr)) {
								// The parameter is unknown, but this isn't an error, so we simply
								// move on
								continue;
							}
							throw new ParserException();
						}

						// Re-set the state
						current = next;
					} finally {
						// Regardless of the outcome, the currentArgs list needs to be cleared
						currentArgs.clear();
					}
					continue;

				case PLAIN:
					// Are we processing a parameter?
					if (current != null) {
						// Can this be considered an argument?
						if ((current.getValueCount() < 0) || (currentArgs.size() < current.getValueCount())) {
							// This token is an argument...stow it as such
							currentArgs.addAll(Parser.split(multiValueSplit, token.value));
							continue;
						}

						// This isn't an argument...so...it's either a subcommand, a trailing
						// value, or an out-of-place value
					}

					// This token is not an argument b/c this parameter doesn't support
					// them (or we have no parameter)...so it's either a subcommand, or one of the
					// trailing command-line values...

					ParameterSet sub = params.getSub(token.value);
					if (sub == null) {
						// Not a subcommand, so ... is this a trailing argument?
						if (i < tokens.lastParameter) {
							if (listener.errorOrphanedValue(token.value)) { throw new ParserException(); }
							// Orphaned value, but not causing a failure
							continue;
						}

						// We're OK..this is a trailing value...
						currentArgs.addAll(Parser.split(multiValueSplit, token.value));
						continue;
					}

					// This is a subcommand, so we replace the current one with this one
					params = sub;
					listener.subCommandFound(token.sourceStr);
					continue;
			}
		}

		if (!currentArgs.isEmpty()) {
			listener.extraArguments(currentArgs);
		}
	}
}