package com.armedia.caliente.cli.parser;

import java.io.File;

public class MissingParameterValueException extends ParserSyntaxException {
	private static final long serialVersionUID = 1L;

	private final Parameter parameter;

	public MissingParameterValueException(File sourceFile, int index, Parameter parameter) {
		super(sourceFile, index);
		this.parameter = parameter;
	}

	public final Parameter getParameter() {
		return this.parameter;
	}
}