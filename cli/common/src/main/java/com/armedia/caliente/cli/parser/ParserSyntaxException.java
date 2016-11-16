package com.armedia.caliente.cli.parser;

import java.io.File;

public class ParserSyntaxException extends ParserException {
	private static final long serialVersionUID = 1L;

	private final File sourceFile;
	private final int index;

	protected ParserSyntaxException(File sourceFile, int index) {
		this.sourceFile = sourceFile;
		this.index = index;
	}

	public final File getSourceFile() {
		return this.sourceFile;
	}

	public final int getIndex() {
		return this.index;
	}
}