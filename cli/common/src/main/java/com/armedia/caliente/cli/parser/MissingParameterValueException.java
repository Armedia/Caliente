package com.armedia.caliente.cli.parser;

public class MissingParameterValueException extends TokenSyntaxException {
	private static final long serialVersionUID = 1L;

	private final Parameter parameter;

	public MissingParameterValueException(TokenSource source, int index, Parameter parameter) {
		super(source, index);
		this.parameter = parameter;
	}

	public final Parameter getParameter() {
		return this.parameter;
	}
}