package com.armedia.caliente.cli.token;

import com.armedia.commons.utilities.Tools;

/**
 * <p>
 * An object signifying a token that will be part of the parameter stream. It indicates not only the
 * {@link TokenType tokenType of the token}, information about where it was read from (the main parameter
 * stream or a parameter file), as well as its relative position within that sourceStr (index within
 * the parameter stream, or line number within the parameter file).
 *
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public final class Token {

	/**
	 * <p>
	 * The {@link TokenSource} from which the token was read. If the value is {@code null}, it means
	 * it was read from the command line.
	 * </p>
	 */
	final TokenSource source;

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
	 * The tokenType of the token
	 * </p>
	 */
	final TokenType tokenType;

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
	final String rawString;

	Token(TokenSource tokenSource, int index, TokenType tokenType, String value, String rawString) {
		this.source = tokenSource;
		this.index = index;
		this.tokenType = tokenType;
		this.value = value;
		this.rawString = rawString;
	}

	public TokenSource getSource() {
		return this.source;
	}

	public int getIndex() {
		return this.index;
	}

	public TokenType getType() {
		return this.tokenType;
	}

	public String getValue() {
		return this.value;
	}

	public String getRawString() {
		return this.rawString;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.tokenType, this.value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { return true; }
		if (!Tools.baseEquals(this, obj)) { return false; }
		Token other = Token.class.cast(obj);
		if (this.tokenType != other.tokenType) { return false; }
		if (!Tools.equals(this.value, other.value)) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format("Token [source=%s, index=%s, tokenType=%s, value=%s, rawString=%s]", this.source, this.index,
			this.tokenType.name(), this.value, this.rawString);
	}
}