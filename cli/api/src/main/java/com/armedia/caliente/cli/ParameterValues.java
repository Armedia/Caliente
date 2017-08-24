package com.armedia.caliente.cli;

import java.util.List;

public interface ParameterValues extends Iterable<ParameterValue> {

	/* General use */
	/**
	 * Returns the {@link ParameterScheme} instance which was used when parsing the values
	 * encapsulated by this instance.
	 *
	 * @return the {@link ParameterScheme} instance which was used when parsing the values
	 *         encapsulated by this instance.
	 */
	public ParameterScheme getParameterScheme();

	/**
	 * Returns {@code true} if the given parameter is defined in the underlying parameter scheme,
	 * {@code false} otherwise.
	 *
	 * @param param
	 * @return {@code true} if the given parameter is defined in the underlying parameter scheme,
	 *         {@code false} otherwise.
	 */
	public boolean isDefined(ParameterDefinition param);

	/**
	 * Returns {@code true} if the given parameter occurs in the parsed command line, {@code false}
	 * otherwise. If this method returns {@code true}, by definition
	 * {@link #getOccurrences(ParameterDefinition)} also returns a value greater than {@code 0}. If
	 * it returns {@code false}, then concordantly {@link #getOccurrences(ParameterDefinition)} must
	 * return {@code 0}.
	 *
	 * @param param
	 * @return {@code true} if the given parameter occurs in the parsed command line, {@code false}
	 *         otherwise
	 */
	public boolean isPresent(ParameterDefinition param);

	/**
	 * Returns the value(s) parsed for the given parameter in the parsed command line.
	 *
	 * @param param
	 * @return the value(s) parsed for the given parameter in the parsed command line.
	 */
	public ParameterValue getParameter(ParameterDefinition param);

	/**
	 * Returns the number of times this parameter occurs in the parsed command line.
	 *
	 * @param param
	 * @return the number of times this parameter occurs in the parsed command line.
	 */
	public int getOccurrences(ParameterDefinition param);

	/* Short Options */
	public Iterable<Character> shortOptions();

	public ParameterValue getParameter(char shortOpt);

	public boolean hasParameter(char shortOpt);

	/* Long Options */
	public Iterable<ParameterValue> longOptions();

	public ParameterValue getParameter(String longOpt);

	public boolean hasParameter(String longOpt);

	/* Booleans */
	public Boolean getBoolean(ParameterDefinition param);

	public Boolean getBoolean(ParameterDefinition param, Boolean def);

	public List<Boolean> getAllBooleans(ParameterDefinition param);

	/* Integers */
	public Integer getInteger(ParameterDefinition param);

	public Integer getInteger(ParameterDefinition param, Integer def);

	public List<Integer> getAllIntegers(ParameterDefinition param);

	/* Longs */
	public Long getLong(ParameterDefinition param);

	public Long getLong(ParameterDefinition param, Long def);

	public List<Long> getAllLongs(ParameterDefinition param);

	/* Floats */
	public Float getFloat(ParameterDefinition param);

	public Float getFloat(ParameterDefinition param, Float def);

	public List<Float> getAllFloats(ParameterDefinition param);

	/* Doubles */
	public Double getDouble(ParameterDefinition param);

	public Double getDouble(ParameterDefinition param, Double def);

	public List<Double> getAllDoubles(ParameterDefinition param);

	/* Strings */
	public String getString(ParameterDefinition param);

	public String getString(ParameterDefinition param, String def);

	public List<String> getAllStrings(ParameterDefinition param);

	/* Same as all above, but for ParameterWrapper */

	/* General use */
	public boolean isDefined(ParameterWrapper param);

	public boolean isPresent(ParameterWrapper param);

	public ParameterValue getParameter(ParameterWrapper param);

	public int getOccurrences(ParameterWrapper param);

	/* Booleans */
	public Boolean getBoolean(ParameterWrapper param);

	public Boolean getBoolean(ParameterWrapper param, Boolean def);

	public List<Boolean> getAllBooleans(ParameterWrapper param);

	/* Integers */
	public Integer getInteger(ParameterWrapper param);

	public Integer getInteger(ParameterWrapper param, Integer def);

	public List<Integer> getAllIntegers(ParameterWrapper param);

	/* Longs */
	public Long getLong(ParameterWrapper param);

	public Long getLong(ParameterWrapper param, Long def);

	public List<Long> getAllLongs(ParameterWrapper param);

	/* Floats */
	public Float getFloat(ParameterWrapper param);

	public Float getFloat(ParameterWrapper param, Float def);

	public List<Float> getAllFloats(ParameterWrapper param);

	/* Doubles */
	public Double getDouble(ParameterWrapper param);

	public Double getDouble(ParameterWrapper param, Double def);

	public List<Double> getAllDoubles(ParameterWrapper param);

	/* Strings */
	public String getString(ParameterWrapper param);

	public String getString(ParameterWrapper param, String def);

	public List<String> getAllStrings(ParameterWrapper param);

}