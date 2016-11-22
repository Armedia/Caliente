package com.armedia.caliente.cli.parser;

import java.util.Collection;
import java.util.List;

public interface ParameterData {

	public String getName();

	public void addNamedValues(Parameter parameter, List<String> values)
		throws TooManyParameterValuesException, MissingParameterValuesException;

	public Collection<Parameter> getParameters();

	public List<String> getValues(Parameter parameter);

	public List<String> getPositionalValues();

	public void setPositionalValues(List<String> values);
}