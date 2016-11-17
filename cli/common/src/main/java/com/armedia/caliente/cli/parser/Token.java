package com.armedia.caliente.cli.parser;

import java.io.File;

/**
 * <p>
 * An object signifying a token that will be part of the parameter stream. It indicates not only the
 * {@link Type type of the token}, information about where it was read from (the main parameter
 * stream or a parameter file), as well as its relative position within that sourceStr (index within
 * the parameter stream, or line number within the parameter file).
 *
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class Token {

	public static enum Type {
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
	 * The file from which the token was read. The value {@code null} means that it was read from
	 * the main parameter stream.
	 * </p>
	 */
	final File sourceFile;

	/**
	 * <p>
	 * The index from within the sourceStr that the token was read. When {@link #sourceFile} is
	 * {@code null}, this means the index within the parameter stream. Otherwise, it means the line
	 * number within the file.
	 * </p>
	 */
	final int index;

	/**
	 * <p>
	 * The type of the token
	 * </p>
	 */
	final Type type;

	/**
	 * <p>
	 * The token's value, minus any applicable prefixes.
	 * </p>
	 */
	final String value;

	/**
	 * <p>
	 * The original string the token was derived from
	 * </p>
	 */
	final String sourceStr;

	Token(File sourceFile, int index, Type type, String value, String source) {
		this.sourceFile = sourceFile;
		this.index = index;
		this.type = type;
		this.value = value;
		this.sourceStr = source;
	}

	@Override
	public String toString() {
		return String.format("Token [sourceFile=%s, index=%s, type=%s, value=%s, sourceStr=%s]", this.sourceFile,
			this.index, this.type.name(), this.value, this.sourceStr);
	}
}