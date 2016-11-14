package com.armedia.caliente.cli.parser;

import java.util.List;
import java.util.Set;

import com.armedia.commons.utilities.Tools;

public final class CommandLineParameter extends BaseParameterDefinition implements Comparable<CommandLineParameter> {
	private final String cliKey;
	private final CommandLine cli;
	private final ParameterDefinition def;

	CommandLineParameter(CommandLine cli, ParameterDefinition def) {
		this.cli = cli;
		this.def = new ImmutableParameterDefinition(def);
		this.cliKey = BaseParameterDefinition.calculateKey(def);
	}

	@Override
	public String getKey() {
		return this.cliKey;
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
	public int getValueCount() {
		return this.def.getValueCount();
	}

	@Override
	public boolean isValueOptional() {
		return this.def.isValueOptional();
	}

	@Override
	public Set<String> getAllowedValues() {
		return this.def.getAllowedValues();
	}

	@Override
	public int compareTo(CommandLineParameter o) {
		if (o == null) { return 1; }
		return Tools.compare(this.cliKey, o.cliKey);
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.cli, this.cliKey);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		CommandLineParameter other = CommandLineParameter.class.cast(obj);
		if (!Tools.equals(this.cliKey, other.cliKey)) { return false; }
		if (this.cli != other.cli) { return false; }
		// TODO: enable comparison of the full definitions?
		/*
		if (!this.def.isEqual(other.def)) { return false; }
		*/
		return true;
	}

	public ParameterDefinition getDefinition() {
		return this.def;
	}

	public CommandLine getCLI() {
		return this.cli;
	}

	public boolean isPresent() {
		return this.cli.isPresent(this);
	}

	public Boolean getBoolean() {
		return this.cli.getBoolean(this);
	}

	public boolean getBoolean(boolean def) {
		return this.cli.getBoolean(this, def);
	}

	public List<Boolean> getAllBooleans() {
		return this.cli.getAllBooleans(this);
	}

	public Integer getInteger() {
		return this.cli.getInteger(this);
	}

	public int getInteger(int def) {
		return this.cli.getInteger(this, def);
	}

	public List<Integer> getAllIntegers() {
		return this.cli.getAllIntegers(this);
	}

	public Long getLong() {
		return this.cli.getLong(this);
	}

	public long getLong(long def) {
		return this.cli.getLong(this, def);
	}

	public List<Long> getAllLongs() {
		return this.cli.getAllLongs(this);
	}

	public Float getFloat() {
		return this.cli.getFloat(this);
	}

	public float getFloat(float def) {
		return this.cli.getFloat(this, def);
	}

	public List<Float> getAllFloats() {
		return this.cli.getAllFloats(this);
	}

	public Double getDouble() {
		return this.cli.getDouble(this);
	}

	public double getDouble(double def) {
		return this.cli.getDouble(this, def);
	}

	public List<Double> getAllDoubles() {
		return this.cli.getAllDoubles(this);
	}

	public String getString() {
		return this.cli.getString(this);
	}

	public String getString(String def) {
		return this.cli.getString(this, def);
	}

	public List<String> getAllStrings() {
		return this.cli.getAllStrings(this);
	}

	@Override
	public boolean isEqual(ParameterDefinition other) {
		return this.def.isEqual(other);
	}
}