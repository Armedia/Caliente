package com.armedia.caliente.cli.parser.token;

import com.armedia.caliente.cli.ParameterDefinition;

public class RequiredParameterMissingException extends TokenProcessorException {
	private static final long serialVersionUID = 1L;

	private final ParameterDefinition parameterDefinition;

	public RequiredParameterMissingException(ParameterDefinition parameterDefinition) {
		this.parameterDefinition = parameterDefinition;
	}

	public final ParameterDefinition getParameter() {
		return this.parameterDefinition;
	}
}