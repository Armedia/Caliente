package com.armedia.caliente.cli;

import java.util.List;

public final class OptionValue extends Option {
	private final OptionValues values;
	private final Option def;

	OptionValue(OptionValues values, Option def) {
		this.values = values;
		this.def = def.clone();
	}

	public Option getDefinition() {
		return this.def;
	}

	@Override
	public OptionValue clone() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getKey() {
		return this.def.getKey();
	}

	@Override
	public boolean isRequired() {
		return this.def.isRequired();
	}

	@Override
	public String getDescription() {
		return this.def.getDescription();
	}

	@Override
	public String getLongOpt() {
		return this.def.getLongOpt();
	}

	@Override
	public Character getShortOpt() {
		return this.def.getShortOpt();
	}

	@Override
	public Character getValueSep() {
		return this.def.getValueSep();
	}

	@Override
	public String getArgumentName() {
		return this.def.getArgumentName();
	}

	@Override
	public int getMinArguments() {
		return this.def.getMinArguments();
	}

	@Override
	public int getMaxArguments() {
		return this.def.getMaxArguments();
	}

	@Override
	public boolean isValuesCaseSensitive() {
		return this.def.isValuesCaseSensitive();
	}

	@Override
	public boolean isValueAllowed(String value) {
		return this.def.isValueAllowed(value);
	}

	@Override
	public OptionValueFilter getValueFilter() {
		return this.def.getValueFilter();
	}

	@Override
	public String getDefault() {
		return this.def.getDefault();
	}

	@Override
	public List<String> getDefaults() {
		return this.def.getDefaults();
	}

	public OptionValues getOptionValues() {
		return this.values;
	}

	public int getOccurrences() {
		return this.values.getOccurrences(this);
	}

	public boolean isPresent() {
		return this.values.isPresent(this);
	}

	public Boolean getBoolean() {
		return getBoolean(null);
	}

	public Boolean getBoolean(Boolean def) {
		return this.values.getBoolean(this, def);
	}

	public List<Boolean> getAllBooleans() {
		return this.values.getAllBooleans(this);
	}

	public Integer getInteger() {
		return getInteger(null);
	}

	public Integer getInteger(Integer def) {
		return this.values.getInteger(this, def);
	}

	public List<Integer> getAllIntegers() {
		return this.values.getAllIntegers(this);
	}

	public Long getLong() {
		return getLong(null);
	}

	public Long getLong(Long def) {
		return this.values.getLong(this, def);
	}

	public List<Long> getAllLongs() {
		return this.values.getAllLongs(this);
	}

	public Float getFloat() {
		return getFloat(null);
	}

	public Float getFloat(Float def) {
		return this.values.getFloat(this, def);
	}

	public List<Float> getAllFloats() {
		return this.values.getAllFloats(this);
	}

	public Double getDouble() {
		return getDouble(null);
	}

	public Double getDouble(Double def) {
		return this.values.getDouble(this, def);
	}

	public List<Double> getAllDoubles() {
		return this.values.getAllDoubles(this);
	}

	public String getString() {
		return getString(null);
	}

	public String getString(String def) {
		return this.values.getString(this, def);
	}

	public List<String> getAllStrings() {
		return this.values.getAllStrings(this);
	}

	public int getValueCount() {
		return this.values.getValueCount(this);
	}

	public boolean hasValues() {
		return this.values.hasValues(this);
	}
}