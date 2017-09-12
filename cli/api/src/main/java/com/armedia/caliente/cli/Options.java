package com.armedia.caliente.cli;

public abstract class Options {

	public final OptionGroup asGroup() {
		return asGroup(null);
	}

	public abstract OptionGroup asGroup(String name);
}