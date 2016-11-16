package com.armedia.caliente.cli.parser;

import java.io.File;

public class ParserFileAccessException extends ParserFileException {
	private static final long serialVersionUID = 1L;

	public ParserFileAccessException(File loopedFile, String message) {
		super(loopedFile, message);
	}

	public ParserFileAccessException(File loopedFile, Throwable thrown) {
		super(loopedFile, thrown);
	}

	public ParserFileAccessException(File loopedFile, String message, Throwable thrown) {
		super(loopedFile, message, thrown);
	}
}