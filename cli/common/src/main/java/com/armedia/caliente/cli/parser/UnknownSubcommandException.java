package com.armedia.caliente.cli.parser;

import java.io.File;

public class UnknownSubcommandException extends ParserException {
	private static final long serialVersionUID = 1L;

	private final File sourceFile;
	private final int sourcePos;
	private final String string;

	public UnknownSubcommandException(File sourceFile, int sourcePos, String string) {
		this.sourceFile = sourceFile;
		this.sourcePos = sourcePos;
		this.string = string;
	}

	public final File getSourceFile() {
		return this.sourceFile;
	}

	public final int getSourcePos() {
		return this.sourcePos;
	}

	public final String getString() {
		return this.string;
	}
}
