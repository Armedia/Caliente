package com.armedia.caliente.cli.parser;

import java.io.File;

public class ParserFileException extends ParserException {
	private static final long serialVersionUID = 1L;

	private final File file;

	protected ParserFileException(File file) {
		super();
		this.file = file;
	}

	public ParserFileException(File file, String message) {
		super(message);
		this.file = file;
	}

	public ParserFileException(File file, Throwable cause) {
		super(cause);
		this.file = file;
	}

	public ParserFileException(File file, String message, Throwable cause) {
		super(message, cause);
		this.file = file;
	}

	public final File getFile() {
		return this.file;
	}
}