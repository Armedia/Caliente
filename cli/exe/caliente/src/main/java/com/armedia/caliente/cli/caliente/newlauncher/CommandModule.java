package com.armedia.caliente.cli.caliente.newlauncher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.armedia.caliente.cli.OptionValues;
import com.armedia.commons.utilities.Tools;

public abstract class CommandModule implements AutoCloseable {

	protected final String name;
	protected final OptionValues commandValues;
	protected final List<String> positionals;

	protected CommandModule(String name, OptionValues commandValues, Collection<String> positionals) {
		this.name = name;
		this.commandValues = commandValues;
		if ((positionals != null) && !positionals.isEmpty()) {
			this.positionals = Tools.freezeList(new ArrayList<>(positionals));
		} else {
			this.positionals = Collections.emptyList();
		}
	}

	public OptionValues getCommandValues() {
		return this.commandValues;
	}

	public List<String> getPositionals() {
		return this.positionals;
	}

	public final String getName() {
		return this.name;
	}

	public abstract int run() throws Exception;

}