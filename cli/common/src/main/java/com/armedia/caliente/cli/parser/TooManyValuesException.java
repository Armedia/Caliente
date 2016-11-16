package com.armedia.caliente.cli.parser;

import java.io.File;
import java.util.List;

import com.armedia.commons.utilities.Tools;

public class TooManyValuesException extends ParserSyntaxException {
	private static final long serialVersionUID = 1L;

	private final Parameter parameter;
	private final List<String> values;

	public TooManyValuesException(File sourceFile, int index, Parameter parameter, List<String> values) {
		super(sourceFile, index);
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