package com.armedia.caliente.cli;

import java.util.List;

import com.armedia.caliente.cli.parser.CommandLineParameter;

public interface CommandLineValues extends Iterable<CommandLineParameter> {

	public boolean isHelpRequested();

	public String getHelpMessage();

	public Iterable<CommandLineParameter> shortOptions();

	public CommandLineParameter getParameter(char shortOpt);

	public boolean hasParameter(char shortOpt);

	public Iterable<CommandLineParameter> longOptions();

	public CommandLineParameter getParameter(String longOpt);

	public boolean hasParameter(String longOpt);

	public boolean isDefined(Parameter param);

	public CommandLineParameter getParameter(Parameter param);

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

	public boolean isDefined(ParameterWrapper wrapper);

	public CommandLineParameter getParameter(ParameterWrapper wrapper);

	public Boolean getBoolean(ParameterWrapper wrapper);

	public boolean getBoolean(ParameterWrapper wrapper, boolean def);

	public List<Boolean> getAllBooleans(ParameterWrapper wrapper);

	public Integer getInteger(ParameterWrapper wrapper);

	public int getInteger(ParameterWrapper wrapper, int def);

	public List<Integer> getAllIntegers(ParameterWrapper wrapper);

	public Long getLong(ParameterWrapper wrapper);

	public long getLong(ParameterWrapper wrapper, long def);

	public List<Long> getAllLongs(ParameterWrapper wrapper);

	public Float getFloat(ParameterWrapper wrapper);

	public float getFloat(ParameterWrapper wrapper, float def);

	public List<Float> getAllFloats(ParameterWrapper wrapper);

	public Double getDouble(ParameterWrapper wrapper);

	public double getDouble(ParameterWrapper wrapper, double def);

	public List<Double> getAllDoubles(ParameterWrapper wrapper);

	public String getString(ParameterWrapper wrapper);

	public String getString(ParameterWrapper wrapper, String def);

	public List<String> getAllStrings(ParameterWrapper wrapper);

	public List<String> getAllStrings(ParameterWrapper wrapper, List<String> def);

	public boolean isPresent(ParameterWrapper wrapper);
}