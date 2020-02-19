/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.cli.token;

import java.util.Objects;

import com.armedia.commons.utilities.Tools;

/**
 * <p>
 * An object signifying a token that will be part of the command line parameter stream. It indicates
 * not only the {@link Type type of the token}, information about where it was read from (the main
 * parameter stream or a parameter file), as well as its relative position within that sourceStr
 * (index within the parameter stream, or line number within the parameter file).
 *
 *
 *
 */
public final class Token {

	public enum Type {
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
		STRING,
		//
		;
	}

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
	final String rawString;

	Token(TokenSource tokenSource, int index, Type type, String value, String rawString) {
		this.source = tokenSource;
		this.index = index;
		this.type = type;
		this.value = value;
		this.rawString = rawString;
	}

	public TokenSource getSource() {
		return this.source;
	}

	public int getIndex() {
		return this.index;
	}

	public Type getType() {
		return this.type;
	}

	public String getValue() {
		return this.value;
	}

	public String getRawString() {
		return this.rawString;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.type, this.value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { return true; }
		if (!Tools.baseEquals(this, obj)) { return false; }
		Token other = Token.class.cast(obj);
		if (this.type != other.type) { return false; }
		if (!Objects.equals(this.value, other.value)) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format("Token [source=%s, index=%s, type=%s, value=%s, rawString=%s]", this.source, this.index,
			this.type.name(), this.value, this.rawString);
	}
}