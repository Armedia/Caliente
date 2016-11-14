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

	public boolean isDefined(Parameter parameter);

	public CommandLineParameter getParameter(Parameter parameter);

	public CommandLineParameter getParameterByKey(String key);

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

	public List<String> getRemainingParameters();

}