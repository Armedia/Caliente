package com.armedia.caliente.cli.parser;

import java.util.List;

import com.armedia.commons.utilities.Tools;

public final class Parameter implements Comparable<Parameter> {
	private final String cliKey;
	private final CommandLine cli;
	private final ParameterDefinition def;

	Parameter(CommandLine cli, ParameterDefinition def) {
		this.cli = cli;
		this.def = new MutableParameterDefinition(def);
		// This bit assumes that the other supporting code will not allow long options with fewer
		// than 2 characters to be defined (why on earth would you want that?!?!)
		String cliKey = def.getLongOpt();
		String prefix = "--";
		if (cliKey == null) {
			cliKey = def.getShortOpt().toString();
			prefix = "-";
		}
		this.cliKey = String.format("%s%s", prefix, cliKey);
	}

	String getKey() {
		return this.cliKey;
	}

	@Override
	public int compareTo(Parameter o) {
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
		Parameter other = Parameter.class.cast(obj);
		if (this.cli != other.cli) { return false; }
		if (!Tools.equals(this.cliKey, other.cliKey)) { return false; }
		// TODO: enable comparison of the full definitions?
		/*
		if (!Tools.equals(this.def, other.def)) { return false; }
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
}