package com.armedia.caliente.cli.parser;

import java.util.List;

public final class Parameter implements ParameterDefinition {
	private final CommandLine cli;
	private final ParameterDefinition def;

	Parameter(CommandLine cli, ParameterDefinition def) {
		this.cli = cli;
		this.def = def;
	}

	@Override
	public int getParameterCount() {
		return this.def.getParameterCount();
	}

	@Override
	public String getArgName() {
		return this.def.getArgName();
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
	public boolean isOptionalArg() {
		return this.def.isOptionalArg();
	}

	final ParameterDefinition getDef() {
		return this.def;
	}

	final CommandLine getCLI() {
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
		return this.cli.getAllBoolean(this);
	}

	public final Integer getInteger() {
		return this.cli.getInteger(this);
	}

	public final int getInteger(int def) {
		return this.cli.getInteger(this, def);
	}

	public final List<Integer> getAllIntegers() {
		return this.cli.getAllInteger(this);
	}

	public final Double getDouble() {
		return this.cli.getDouble(this);
	}

	public final double getDouble(double def) {
		return this.cli.getDouble(this, def);
	}

	public final List<Double> getAllDoubles() {
		return this.cli.getAllDouble(this);
	}

	public final String getString() {
		return this.cli.getString(this);
	}

	public final String getString(String def) {
		return this.cli.getString(this, def);
	}

	public final List<String> getAllStrings() {
		return this.cli.getAllString(this);
	}
}