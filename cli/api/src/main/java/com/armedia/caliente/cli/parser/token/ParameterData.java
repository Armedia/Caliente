package com.armedia.caliente.cli.parser.token;

import java.util.Collection;
import java.util.List;

import com.armedia.caliente.cli.ParameterDefinition;

public interface ParameterData {

	public String getName();

	public void addNamedValues(ParameterDefinition parameterDefinition, List<String> values)
		throws TooManyParameterValuesException, MissingParameterValuesException;

	public Collection<ParameterDefinition> getParameters();

	public List<String> getValues(ParameterDefinition parameterDefinition);

	public List<String> getPositionalValues();

	public void setPositionalValues(List<String> values);
}