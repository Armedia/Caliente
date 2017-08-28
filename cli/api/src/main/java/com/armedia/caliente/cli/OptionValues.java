package com.armedia.caliente.cli;

import java.util.Collection;
import java.util.List;

public interface OptionValues extends Iterable<OptionValue> {

	/* General use */
	/**
	 * Returns {@code true} if the given option is defined in the underlying option scheme,
	 * {@code false} otherwise.
	 *
	 * @param param
	 * @return {@code true} if the given option is defined in the underlying option scheme,
	 *         {@code false} otherwise.
	 */
	public boolean isDefined(Option param);

	/**
	 * Returns {@code true} if the given option occurs in the parsed command line, {@code false}
	 * otherwise. If this method returns {@code true}, by definition {@link #getOccurrences(Option)}
	 * also returns a value greater than {@code 0}. If it returns {@code false}, then concordantly
	 * {@link #getOccurrences(Option)} must return {@code 0}.
	 *
	 * @param param
	 * @return {@code true} if the given option occurs in the parsed command line, {@code false}
	 *         otherwise
	 */
	public boolean isPresent(Option param);

	/**
	 * Returns the value(s) parsed for the given option in the parsed command line.
	 *
	 * @param param
	 * @return the value(s) parsed for the given option in the parsed command line.
	 */
	public OptionValue getOptionValue(Option param);

	/**
	 * Returns the number of times this option occurs in the parsed command line.
	 *
	 * @param param
	 * @return the number of times this option occurs in the parsed command line.
	 */
	public int getOccurrences(Option param);

	/**
	 * Returns the string paramters given with the specified occurrence (0-index) of the option. It
	 * will return {@code null} if the option has not been given, and will raise an
	 * {@link IndexOutOfBoundsException} if there occurrence number requested is higher than the
	 * number of present occurrences
	 *
	 * @param param
	 * @param occurrence
	 * @return the string paramters given with the specified occurrence of the option
	 */
	public Collection<String> getOccurrenceValues(Option param, int occurrence);

	/**
	 * Returns the number of values associated with the given option across all occurrences.
	 *
	 * @param param
	 * @return the number of values associated with the given option across all occurrences.
	 */
	public int getValueCount(Option param);

	/* Short Options */
	public Iterable<OptionValue> shortOptions();

	public OptionValue getOption(char shortOpt);

	public boolean hasOption(char shortOpt);

	/* Long Options */
	public Iterable<OptionValue> longOptions();

	public OptionValue getOption(String longOpt);

	public boolean hasOption(String longOpt);

	/* Booleans */
	public Boolean getBoolean(Option param);

	public Boolean getBoolean(Option param, Boolean def);

	public List<Boolean> getAllBooleans(Option param);

	/* Integers */
	public Integer getInteger(Option param);

	public Integer getInteger(Option param, Integer def);

	public List<Integer> getAllIntegers(Option param);

	/* Longs */
	public Long getLong(Option param);

	public Long getLong(Option param, Long def);

	public List<Long> getAllLongs(Option param);

	/* Floats */
	public Float getFloat(Option param);

	public Float getFloat(Option param, Float def);

	public List<Float> getAllFloats(Option param);

	/* Doubles */
	public Double getDouble(Option param);

	public Double getDouble(Option param, Double def);

	public List<Double> getAllDoubles(Option param);

	/* Strings */
	public String getString(Option param);

	public String getString(Option param, String def);

	public List<String> getAllStrings(Option param);

	/* Same as all above, but for OptionWrapper */

	/* General use */
	public boolean isDefined(OptionWrapper param);

	public boolean isPresent(OptionWrapper param);

	public OptionValue getOption(OptionWrapper param);

	public int getOccurrences(OptionWrapper param);

	public Collection<String> getOccurrenceValues(OptionWrapper param, int occurrence);

	public int getValueCount(OptionWrapper param);

	/* Booleans */
	public Boolean getBoolean(OptionWrapper param);

	public Boolean getBoolean(OptionWrapper param, Boolean def);

	public List<Boolean> getAllBooleans(OptionWrapper param);

	/* Integers */
	public Integer getInteger(OptionWrapper param);

	public Integer getInteger(OptionWrapper param, Integer def);

	public List<Integer> getAllIntegers(OptionWrapper param);

	/* Longs */
	public Long getLong(OptionWrapper param);

	public Long getLong(OptionWrapper param, Long def);

	public List<Long> getAllLongs(OptionWrapper param);

	/* Floats */
	public Float getFloat(OptionWrapper param);

	public Float getFloat(OptionWrapper param, Float def);

	public List<Float> getAllFloats(OptionWrapper param);

	/* Doubles */
	public Double getDouble(OptionWrapper param);

	public Double getDouble(OptionWrapper param, Double def);

	public List<Double> getAllDoubles(OptionWrapper param);

	/* Strings */
	public String getString(OptionWrapper param);

	public String getString(OptionWrapper param, String def);

	public List<String> getAllStrings(OptionWrapper param);

}