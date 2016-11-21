package com.armedia.caliente.cli.parser;

public class RequiredParameterMissingException extends TokenProcessorException {
	private static final long serialVersionUID = 1L;

	private final Parameter parameter;

	public RequiredParameterMissingException(Parameter parameter) {
		this.parameter = parameter;
	}

	public final Parameter getParameter() {
		return this.parameter;
	}
}