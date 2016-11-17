package com.armedia.caliente.cli.parser;

import java.util.Set;
import java.util.TreeSet;

import com.armedia.commons.utilities.Tools;

public final class MutableParameter extends BaseParameter implements Cloneable {

	protected boolean required = false;
	protected String description = null;
	protected Character shortOpt = null;
	protected String longOpt = null;
	protected int minValueCount = 0;
	protected int maxValueCount = 0;
	protected String valueName = null;
	protected Character valueSep = MutableParameter.DEFAULT_VALUE_SEP;
	protected Set<String> allowedValues = new TreeSet<>();

	public MutableParameter() {
	}

	MutableParameter(Parameter other) {
		this.required = other.isRequired();
		this.description = other.getDescription();
		this.shortOpt = other.getShortOpt();
		this.longOpt = other.getLongOpt();
		this.minValueCount = other.getMinValueCount();
		this.maxValueCount = other.getMaxValueCount();
		this.valueName = other.getValueName();
		this.valueSep = other.getValueSep();
		Set<String> allowedValues = other.getAllowedValues();
		if (allowedValues != null) {
			this.allowedValues.addAll(allowedValues);
		}
	}

	@Override
	public MutableParameter clone() {
		return new MutableParameter(this);
	}

	@Override
	public int getMinValueCount() {
		return this.minValueCount;
	}

	public MutableParameter setMinValueCount(int count) {
		this.minValueCount = count;
		return this;
	}

	@Override
	public int getMaxValueCount() {
		return this.maxValueCount;
	}

	public MutableParameter setMaxValueCount(int count) {
		this.maxValueCount = count;
		return this;
	}

	@Override
	public String getValueName() {
		return this.valueName;
	}

	public MutableParameter setValueName(String argName) {
		this.valueName = argName;
		return this;
	}

	@Override
	public boolean isRequired() {
		return this.required;
	}

	public MutableParameter setRequired(boolean required) {
		this.required = required;
		return this;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	public MutableParameter setDescription(String description) {
		this.description = description;
		return this;
	}

	@Override
	public String getLongOpt() {
		return this.longOpt;
	}

	public MutableParameter setLongOpt(String longOpt) {
		this.longOpt = longOpt;
		return this;
	}

	@Override
	public Character getShortOpt() {
		return this.shortOpt;
	}

	public MutableParameter setShortOpt(Character shortOpt) {
		this.shortOpt = shortOpt;
		return this;
	}

	@Override
	public Character getValueSep() {
		return this.valueSep;
	}

	public MutableParameter setValueSep(Character valueSep) {
		this.valueSep = valueSep;
		return this;
	}

	@Override
	public Set<String> getAllowedValues() {
		return this.allowedValues;
	}

	public void setAllowedValues(Set<String> allowedValues) {
		this.allowedValues = allowedValues;
	}

	public Parameter freezeCopy() {
		return new ImmutableParameter(this);
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.required, this.description, this.shortOpt, this.longOpt, this.valueName,
			this.maxValueCount, this.minValueCount, this.valueSep);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		MutableParameter other = MutableParameter.class.cast(obj);
		return isEqual(other);
	}

	@Override
	public boolean isEqual(Parameter other) {
		if (other == null) { return false; }
		if (isRequired() != other.isRequired()) { return false; }
		if (getMinValueCount() != other.getMinValueCount()) { return false; }
		if (getMaxValueCount() != other.getMaxValueCount()) { return false; }
		if (!Tools.equals(getDescription(), other.getDescription())) { return false; }
		if (!Tools.equals(getLongOpt(), other.getLongOpt())) { return false; }
		if (!Tools.equals(getShortOpt(), other.getShortOpt())) { return false; }
		if (!Tools.equals(getValueName(), other.getValueName())) { return false; }
		if (!Tools.equals(getValueSep(), other.getValueSep())) { return false; }
		if (!Tools.equals(getAllowedValues(), other.getAllowedValues())) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format(
			"MutableParameter [required=%s, shortOpt=%s, longOpt=%s, description=%s, minValueCount=%d, maxValueCount=%d, valueName=%s, minValueCount=%s, valueSep=%s]",
			this.required, this.shortOpt, this.longOpt, this.description, this.minValueCount, this.maxValueCount,
			this.valueName, this.minValueCount, this.valueSep);
	}
}