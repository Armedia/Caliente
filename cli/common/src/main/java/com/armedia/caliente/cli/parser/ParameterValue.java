package com.armedia.caliente.cli.parser;

import java.util.List;

import com.armedia.commons.utilities.Tools;

public final class ParameterValue implements ParameterDefinition, Comparable<ParameterValue> {
	private final String cliKey;
	private final CommandLine cli;
	private final ParameterDefinition def;

	ParameterValue(CommandLine cli, ParameterDefinition def) {
		this.cli = cli;
		this.def = def;

		// This bit assumes that the other supporting code will not allow long options with fewer
		// than 2 characters to be defined (why on earth would you want that?!?!)
		String cliKey = def.getLongOpt();
		if (cliKey == null) {
			cliKey = def.getShortOpt().toString();
		}
		this.cliKey = cliKey;
	}

	String getCommandLineKey() {
		return this.cliKey;
	}

	@Override
	public int compareTo(ParameterValue o) {
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
		ParameterValue other = ParameterValue.class.cast(obj);
		if (this.cli != other.cli) { return false; }
		if (!Tools.equals(this.cliKey, other.cliKey)) { return false; }
		// TODO: enable comparison of the full definitions?
		/*
		if (this.def.isRequired() != other.def.isRequired()) { return false; }
		if (this.def.isValueOptional() != other.def.isValueOptional()) { return false; }
		if (!Tools.equals(this.def.getDescription(), other.def.getDescription())) { return false; }
		if (!Tools.equals(this.def.getLongOpt(), other.def.getLongOpt())) { return false; }
		if (!Tools.equals(this.def.getShortOpt(), other.def.getShortOpt())) { return false; }
		if (this.def.getValueCount() != other.def.getValueCount()) { return false; }
		if (!Tools.equals(this.def.getValueName(), other.def.getValueName())) { return false; }
		if (!Tools.equals(this.def.getValueSep(), other.def.getValueSep())) { return false; }
		*/
		return true;
	}

	@Override
	public int getValueCount() {
		return this.def.getValueCount();
	}

	@Override
	public String getValueName() {
		return this.def.getValueName();
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
	public boolean isValueOptional() {
		return this.def.isValueOptional();
	}

	public final ParameterDefinition getDefinition() {
		return this.def;
	}

	public final CommandLine getCLI() {
		return this.cli;
	}

	public final boolean isPresent() {
		return this.cli.isPresent(this);
	}

	public final Boolean getBoolean() {
		return this.cli.getBoolean(this);
	}

	public final boolean getBoolean(boolean def) {
		return this.cli.getBoolean(this, def);
	}

	public final List<Boolean> getAllBooleans() {
		return this.cli.getAllBooleans(this);
	}

	public final Integer getInteger() {
		return this.cli.getInteger(this);
	}

	public final int getInteger(int def) {
		return this.cli.getInteger(this, def);
	}

	public final List<Integer> getAllIntegers() {
		return this.cli.getAllIntegers(this);
	}

	public final Double getDouble() {
		return this.cli.getDouble(this);
	}

	public final double getDouble(double def) {
		return this.cli.getDouble(this, def);
	}

	public final List<Double> getAllDoubles() {
		return this.cli.getAllDoubles(this);
	}

	public final String getString() {
		return this.cli.getString(this);
	}

	public final String getString(String def) {
		return this.cli.getString(this, def);
	}

	public final List<String> getAllStrings() {
		return this.cli.getAllStrings(this);
	}
}