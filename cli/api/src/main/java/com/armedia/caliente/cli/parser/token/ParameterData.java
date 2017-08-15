package com.armedia.caliente.cli.parser.token;

import java.util.Collection;
import java.util.List;

import com.armedia.caliente.cli.Parameter;

public interface ParameterData {

	public String getName();

	public void addNamedValues(Parameter parameter, List<String> values)
		throws TooManyParameterValuesException, MissingParameterValuesException;

	public Collection<Parameter> getParameters();

	public List<String> getValues(Parameter parameter);

	public List<String> getPositionalValues();

	public void setPositionalValues(List<String> values);
}