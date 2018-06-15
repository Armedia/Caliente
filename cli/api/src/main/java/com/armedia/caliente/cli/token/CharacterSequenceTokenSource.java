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
		Objects.requireNonNull(characters, "Must provide a non-null character sequence");
		this.characters = characters;
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