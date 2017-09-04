package com.armedia.caliente.cli;

import java.util.Collection;

import com.armedia.caliente.cli.exception.DuplicateOptionException;

class ExtensibleOptionScheme implements PositionalValueSupport {

	private OptionScheme scheme;
	private OptionScheme changes;
	private String name = null;

	ExtensibleOptionScheme(OptionScheme scheme) {
		this.scheme = new OptionScheme(scheme);
		this.changes = new OptionScheme(scheme.isCaseSensitive()) //
			.setMinArguments(scheme.getMinArguments()) //
			.setMaxArguments(scheme.getMaxArguments()) //
			.setDynamic(scheme.isDynamic());
	}

	OptionScheme getChanges() {
		return this.changes;
	}

	boolean isModified() {
		return (this.changes.getOptionCount() != 0);
	}

	public final ExtensibleOptionScheme setName(String name) {
		this.name = name;
		return this;
	}

	public String getName() {
		return this.name;
	}

	public ExtensibleOptionScheme setDescription(String description) {
		this.changes.setDescription(description);
		return this;
	}

	public String getDescription() {
		return this.changes.getDescription();
	}

	public boolean isSupportsArguments() {
		return this.changes.isSupportsPositionals();
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
		this.changes.add(option);
		return this;
	}

	public Collection<Option> getOptions() {
		return this.changes.getOptions();
	}

	public Collection<Option> getRequiredOptions() {
		return this.changes.getRequiredOptions();
	}

	public int getRequiredOptionCount() {
		return this.changes.getRequiredOptionCount();
	}

	public int getOptionCount() {
		return this.changes.getOptionCount();
	}

	public boolean hasOption(Character shortOpt) {
		return this.changes.hasOption(shortOpt);
	}

	public boolean hasOption(String longOpt) {
		return this.changes.hasOption(longOpt);
	}

	public int countCollisions(Option option) {
		return this.changes.countCollisions(option);
	}

	public Option getOption(String longOpt) {
		return this.changes.getOption(longOpt);
	}

	public Option getOption(Character shortOpt) {
		return this.changes.getOption(shortOpt);
	}
}