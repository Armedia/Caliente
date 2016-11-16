package com.armedia.caliente.cli.parser;

import java.io.File;

public class UnknownParameterException extends ParserSyntaxException {
	private static final long serialVersionUID = 1L;

	private final String string;

	public UnknownParameterException(File sourceFile, int index, String string) {
		super(sourceFile, index);
		this.string = string;
	}

	public final String getString() {
		return this.string;
	}
}
