package com.armedia.caliente.cli;

import java.util.Collection;

import com.armedia.caliente.cli.exception.DuplicateOptionException;

class ExtensibleOptionScheme implements PositionalValueSupport {

	private OptionScheme scheme;
	private boolean modified = false;

	ExtensibleOptionScheme(OptionScheme scheme) {
		this.scheme = new OptionScheme(scheme);
	}

	boolean isModified() {
		return this.modified;
	}

	void clearModified() {
		this.modified = false;
	}

	public String getName() {
		return this.scheme.getName();
	}

	public boolean isSupportsArguments() {
		return this.scheme.isSupportsPositionals();
	}

	@Override
	public String getArgumentName() {
		return this.scheme.getArgumentName();
	}

	@Override
	public int getMinArguments() {
		return this.scheme.getMinArguments();
	}

	@Override
	public int getMaxArguments() {
		return this.scheme.getMaxArguments();
	}

	public ExtensibleOptionScheme add(Option option) throws DuplicateOptionException {
		this.scheme.add(option);
		this.modified = true;
		return this;
	}

	public Collection<Option> getOptions() {
		return this.scheme.getOptions();
	}

	public Collection<Option> getRequiredOptions() {
		return this.scheme.getRequiredOptions();
	}

	public int getRequiredOptionCount() {
		return this.scheme.getRequiredOptionCount();
	}

	public int getOptionCount() {
		return this.scheme.getOptionCount();
	}

	public boolean hasOption(Character shortOpt) {
		return this.scheme.hasOption(shortOpt);
	}

	public boolean hasOption(String longOpt) {
		return this.scheme.hasOption(longOpt);
	}

	public int countCollisions(Option option) {
		return this.scheme.countCollisions(option);
	}

	public Option getOption(String longOpt) {
		return this.scheme.getOption(longOpt);
	}

	public Option getOption(Character shortOpt) {
		return this.scheme.getOption(shortOpt);
	}
}