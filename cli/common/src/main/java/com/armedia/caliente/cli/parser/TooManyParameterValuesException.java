package com.armedia.caliente.cli.parser;

import java.util.List;

import com.armedia.commons.utilities.Tools;

public class TooManyParameterValuesException extends TokenProcessorException {
	private static final long serialVersionUID = 1L;

	private final Parameter parameter;
	private final List<String> values;

	public TooManyParameterValuesException(Parameter parameter, List<String> values) {
		this.parameter = parameter;
		this.values = Tools.freezeCopy(values);
	}

	public final Parameter getParameter() {
		return this.parameter;
	}

	public final List<String> getValues() {
		return this.values;
	}
}