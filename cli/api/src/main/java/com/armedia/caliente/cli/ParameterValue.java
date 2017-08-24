package com.armedia.caliente.cli;

import java.util.List;
import java.util.Set;

public final class ParameterValue extends ParameterDefinition {
	private final ParameterValues values;
	private final ParameterDefinition def;

	ParameterValue(ParameterValues values, ParameterDefinition def) {
		this.values = values;
		this.def = def;
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
	public String getValueName() {
		return this.def.getValueName();
	}

	@Override
	public int getMinValueCount() {
		return this.def.getMinValueCount();
	}

	@Override
	public int getMaxValueCount() {
		return this.def.getMaxValueCount();
	}

	@Override
	public Set<String> getAllowedValues() {
		return this.def.getAllowedValues();
	}

	@Override
	public String getDefault() {
		return this.def.getDefault();
	}

	@Override
	public List<String> getDefaults() {
		return this.def.getDefaults();
	}

	public ParameterValues getParameterValues() {
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
}