package com.armedia.caliente.cli.parser;

import java.util.List;

public interface ParameterProcessor extends ParameterSet {

	public void addNamedValues(Parameter parameter, List<String> values) throws TooManyParameterValuesException;

	public List<String> getNamedValues(Parameter parameter);

	public List<String> getPositionalValues();

	public void setPositionalValues(List<String> values);

	public ParameterErrorPolicy getErrorPolicy();

	public void processingComplete(ParameterErrorPolicy errorPolicy)
		throws TooManyParameterValuesException, MissingParameterValuesException, RequiredParameterMissingException;

}