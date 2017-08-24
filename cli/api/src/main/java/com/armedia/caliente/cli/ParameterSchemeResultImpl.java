package com.armedia.caliente.cli;

import java.util.List;

import com.armedia.commons.utilities.Tools;

public class ParameterSchemeResultImpl implements ParameterSchemeResult {

	private final ParameterValues parameterValues;
	private final List<String> positionals;

	public ParameterSchemeResultImpl(ParameterValues parameterValues, List<String> positionals) {
		this.parameterValues = parameterValues;
		this.positionals = Tools.freezeCopy(positionals, true);
	}

	@Override
	public ParameterValues getParameterValues() {
		return this.parameterValues;
	}

	@Override
	public List<String> getPositionals() {
		return this.positionals;
	}

}
