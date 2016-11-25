package com.armedia.caliente.cli.parser.token;

import java.util.List;

import com.armedia.caliente.cli.parser.Parameter;
import com.armedia.commons.utilities.Tools;

public class MissingParameterValuesException extends TokenProcessorException {
	private static final long serialVersionUID = 1L;

	private final Parameter parameter;
	private final List<String> values;

	public MissingParameterValuesException(Parameter parameter) {
		this(parameter, null);
	}

	public MissingParameterValuesException(Parameter parameter, List<String> values) {
		this.parameter = parameter;
		this.values = Tools.freezeCopy(values, true);
	}

	public final Parameter getParameter() {
		return this.parameter;
	}

	public final List<String> getValues() {
		return this.values;
	}
}