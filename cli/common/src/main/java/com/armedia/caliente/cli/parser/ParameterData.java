package com.armedia.caliente.cli.parser;

import java.util.List;

class ParameterData {

	final ParameterDefinition parameter;
	final List<String> values;

	ParameterData(ParameterDefinition parameter, List<String> values) {
		this.parameter = parameter;
		this.values = values;
	}
}