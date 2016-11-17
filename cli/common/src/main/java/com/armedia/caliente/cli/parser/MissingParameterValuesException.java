package com.armedia.caliente.cli.parser;

import java.util.List;

import com.armedia.commons.utilities.Tools;

public class MissingParameterValuesException extends TokenSyntaxException {
	private static final long serialVersionUID = 1L;

	private final Parameter parameter;
	private final List<String> values;

	public MissingParameterValuesException(TokenSource source, int index, Parameter parameter) {
		this(source, index, parameter, null);
	}

	public MissingParameterValuesException(TokenSource source, int index, Parameter parameter, List<String> values) {
		super(source, index);
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