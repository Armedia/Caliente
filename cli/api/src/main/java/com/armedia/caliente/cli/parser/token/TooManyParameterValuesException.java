package com.armedia.caliente.cli.parser.token;

import java.util.List;

import com.armedia.caliente.cli.ParameterDefinition;
import com.armedia.commons.utilities.Tools;

public class TooManyParameterValuesException extends TokenProcessorException {
	private static final long serialVersionUID = 1L;

	private final ParameterDefinition parameterDefinition;
	private final List<String> values;

	public TooManyParameterValuesException(ParameterDefinition parameterDefinition, List<String> values) {
		this.parameterDefinition = parameterDefinition;
		this.values = Tools.freezeCopy(values);
	}

	public final ParameterDefinition getParameter() {
		return this.parameterDefinition;
	}

	public final List<String> getValues() {
		return this.values;
	}
}