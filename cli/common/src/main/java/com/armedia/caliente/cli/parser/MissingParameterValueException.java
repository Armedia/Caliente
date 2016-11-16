package com.armedia.caliente.cli.parser;

public class MissingParameterValueException extends ParserException {
	private static final long serialVersionUID = 1L;

	private final Parameter parameter;

	public MissingParameterValueException(Parameter parameter) {
		this.parameter = parameter;
	}

	public final Parameter getParameter() {
		return this.parameter;
	}
}