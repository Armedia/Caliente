/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
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

import java.io.IOException;
import java.io.Reader;
import java.util.Objects;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.input.CharSequenceReader;

public class CharacterSequenceTokenSource extends ReaderTokenSource {

	private final CharSequence characters;
	private final String hash;

	public CharacterSequenceTokenSource(CharSequence characters) {
		this.characters = Objects.requireNonNull(characters, "Must provide a non-null character sequence");
		this.hash = DigestUtils.sha256Hex(characters.toString());
	}

	@Override
	public String getKey() {
		return this.hash;
	}

	@Override
	protected Reader openReader() throws IOException {
		return new CharSequenceReader(this.characters);
	}

	@Override
	public String toString() {
		return String.format("CharacterSequenceTokenSource [sequence=%s]", this.characters.toString());
	}
}