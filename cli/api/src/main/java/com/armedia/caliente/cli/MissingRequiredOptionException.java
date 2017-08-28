package com.armedia.caliente.cli;

import java.util.ArrayList;
import java.util.Collection;

import com.armedia.commons.utilities.Tools;

public class MissingRequiredOptionException extends CommandLineSyntaxException {
	private static final long serialVersionUID = 1L;

	private final Collection<Option> baseMissing;
	private final String command;
	private final Collection<Option> commandMissing;

	public MissingRequiredOptionException(Collection<Option> baseMissing, String command,
		Collection<Option> commandMissing) {
		super(null, 0);
		this.baseMissing = (baseMissing.isEmpty() ? null : Tools.freezeCollection(new ArrayList<>(baseMissing)));
		if ((command != null) && (commandMissing != null) && !commandMissing.isEmpty()) {
			this.command = command;
			this.commandMissing = Tools.freezeCollection(new ArrayList<>(commandMissing));
		} else {
			this.command = null;
			this.commandMissing = null;
		}
	}

	public Collection<Option> getBaseMissing() {
		return this.baseMissing;
	}

	public String getCommand() {
		return this.command;
	}

	public Collection<Option> getCommandMissing() {
		return this.commandMissing;
	}
}