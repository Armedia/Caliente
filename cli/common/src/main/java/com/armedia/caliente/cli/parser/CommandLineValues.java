package com.armedia.caliente.cli.parser;

import java.util.Iterator;
import java.util.List;

public interface CommandLineValues {

	boolean isHelpRequested();

	String getHelpMessage();

	Iterator<Parameter> iterator();

	Iterable<Parameter> shortOptions();

	Parameter getParameter(char shortOpt);

	boolean hasParameter(char shortOpt);

	Iterable<Parameter> longOptions();

	Parameter getParameter(String longOpt);

	boolean hasParameter(String longOpt);

	boolean isParameterDefined(ParameterDefinition parameter);

	Parameter getParameterFromDefinition(ParameterDefinition parameter);

	boolean hasHelpParameter();

	Parameter getHelpParameter();

	Boolean getBoolean(ParameterDefinition param);

	boolean getBoolean(ParameterDefinition param, boolean def);

	List<Boolean> getAllBooleans(ParameterDefinition param);

	Integer getInteger(ParameterDefinition param);

	int getInteger(ParameterDefinition param, int def);

	List<Integer> getAllIntegers(ParameterDefinition param);

	Long getLong(ParameterDefinition param);

	long getLong(ParameterDefinition param, long def);

	List<Long> getAllLongs(ParameterDefinition param);

	Float getFloat(ParameterDefinition param);

	float getFloat(ParameterDefinition param, float def);

	List<Float> getAllFloats(ParameterDefinition param);

	Double getDouble(ParameterDefinition param);

	double getDouble(ParameterDefinition param, double def);

	List<Double> getAllDoubles(ParameterDefinition param);

	String getString(ParameterDefinition param);

	String getString(ParameterDefinition param, String def);

	List<String> getAllStrings(ParameterDefinition param);

	boolean isPresent(ParameterDefinition param);

	List<String> getRemainingParameters();

}