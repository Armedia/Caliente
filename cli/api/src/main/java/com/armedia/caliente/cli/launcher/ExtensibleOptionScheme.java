package com.armedia.caliente.cli.launcher;

import java.util.Collection;

import com.armedia.caliente.cli.DuplicateOptionException;
import com.armedia.caliente.cli.InvalidOptionException;
import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionScheme;

class ExtensibleOptionScheme {

	private OptionScheme scheme;
	private boolean modified = false;

	ExtensibleOptionScheme(OptionScheme scheme) {
		this.scheme = new OptionScheme(scheme);
	}

	boolean isModified() {
		return this.modified;
	}

	public String getName() {
		return this.scheme.getName();
	}

	public boolean isSupportsArguments() {
		return this.scheme.isSupportsPositionals();
	}

	public int getMinArgs() {
		return this.scheme.getMinArgs();
	}

	public int getMaxArgs() {
		return this.scheme.getMaxArgs();
	}

	public ExtensibleOptionScheme add(Option option) throws InvalidOptionException, DuplicateOptionException {
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