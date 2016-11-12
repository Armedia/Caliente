package com.armedia.caliente.cli.parser;

import java.util.Iterator;
import java.util.List;

public interface CommandLineValues {

	public boolean isHelpRequested();

	public String getHelpMessage();

	public Iterator<CommandLineParameter> iterator();

	public Iterable<CommandLineParameter> shortOptions();

	public CommandLineParameter getParameter(char shortOpt);

	public boolean hasParameter(char shortOpt);

	public Iterable<CommandLineParameter> longOptions();

	public CommandLineParameter getParameter(String longOpt);

	public boolean hasParameter(String longOpt);

	public boolean isParameterDefined(ParameterDefinition parameter);

	public CommandLineParameter getParameterFromDefinition(ParameterDefinition parameter);

	public boolean hasHelpParameter();

	public CommandLineParameter getHelpParameter();

	public Boolean getBoolean(ParameterDefinition param);

	public boolean getBoolean(ParameterDefinition param, boolean def);

	public List<Boolean> getAllBooleans(ParameterDefinition param);

	public Integer getInteger(ParameterDefinition param);

	public int getInteger(ParameterDefinition param, int def);

	public List<Integer> getAllIntegers(ParameterDefinition param);

	public Long getLong(ParameterDefinition param);

	public long getLong(ParameterDefinition param, long def);

	public List<Long> getAllLongs(ParameterDefinition param);

	public Float getFloat(ParameterDefinition param);

	public float getFloat(ParameterDefinition param, float def);

	public List<Float> getAllFloats(ParameterDefinition param);

	public Double getDouble(ParameterDefinition param);

	public double getDouble(ParameterDefinition param, double def);

	public List<Double> getAllDoubles(ParameterDefinition param);

	public String getString(ParameterDefinition param);

	public String getString(ParameterDefinition param, String def);

	public List<String> getAllStrings(ParameterDefinition param);

	public List<String> getAllStrings(ParameterDefinition param, List<String> def);

	public boolean isPresent(ParameterDefinition param);

	public List<String> getRemainingParameters();

}