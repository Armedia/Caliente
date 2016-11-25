package com.armedia.caliente.cli.parser;

import java.util.List;

public interface CommandLineValues extends Iterable<CommandLineParameter> {

	public boolean isHelpRequested();

	public String getHelpMessage();

	public Iterable<CommandLineParameter> shortOptions();

	public CommandLineParameter getParameter(char shortOpt);

	public boolean hasParameter(char shortOpt);

	public Iterable<CommandLineParameter> longOptions();

	public CommandLineParameter getParameter(String longOpt);

	public boolean hasParameter(String longOpt);

	public boolean isDefined(Parameter parameter);

	public CommandLineParameter getParameter(Parameter parameter);

	public boolean hasHelpParameter();

	public CommandLineParameter getHelpParameter();

	public Boolean getBoolean(Parameter param);

	public boolean getBoolean(Parameter param, boolean def);

	public List<Boolean> getAllBooleans(Parameter param);

	public Integer getInteger(Parameter param);

	public int getInteger(Parameter param, int def);

	public List<Integer> getAllIntegers(Parameter param);

	public Long getLong(Parameter param);

	public long getLong(Parameter param, long def);

	public List<Long> getAllLongs(Parameter param);

	public Float getFloat(Parameter param);

	public float getFloat(Parameter param, float def);

	public List<Float> getAllFloats(Parameter param);

	public Double getDouble(Parameter param);

	public double getDouble(Parameter param, double def);

	public List<Double> getAllDoubles(Parameter param);

	public String getString(Parameter param);

	public String getString(Parameter param, String def);

	public List<String> getAllStrings(Parameter param);

	public List<String> getAllStrings(Parameter param, List<String> def);

	public boolean isPresent(Parameter param);

	public List<String> getPositionalValues();

	public boolean isDefined(ParameterWrapper paramDel);

	public CommandLineParameter getParameter(ParameterWrapper paramDel);

	public Boolean getBoolean(ParameterWrapper paramDel);

	public boolean getBoolean(ParameterWrapper paramDel, boolean def);

	public List<Boolean> getAllBooleans(ParameterWrapper paramDel);

	public Integer getInteger(ParameterWrapper paramDel);

	public int getInteger(ParameterWrapper paramDel, int def);

	public List<Integer> getAllIntegers(ParameterWrapper paramDel);

	public Long getLong(ParameterWrapper paramDel);

	public long getLong(ParameterWrapper paramDel, long def);

	public List<Long> getAllLongs(ParameterWrapper paramDel);

	public Float getFloat(ParameterWrapper paramDel);

	public float getFloat(ParameterWrapper paramDel, float def);

	public List<Float> getAllFloats(ParameterWrapper paramDel);

	public Double getDouble(ParameterWrapper paramDel);

	public double getDouble(ParameterWrapper paramDel, double def);

	public List<Double> getAllDoubles(ParameterWrapper paramDel);

	public String getString(ParameterWrapper paramDel);

	public String getString(ParameterWrapper paramDel, String def);

	public List<String> getAllStrings(ParameterWrapper paramDel);

	public List<String> getAllStrings(ParameterWrapper paramDel, List<String> def);

	public boolean isPresent(ParameterWrapper paramDel);
}