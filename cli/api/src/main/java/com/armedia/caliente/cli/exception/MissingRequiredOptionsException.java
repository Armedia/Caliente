package com.armedia.caliente.cli.exception;

import java.util.ArrayList;
import java.util.Collection;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionScheme;
import com.armedia.commons.utilities.Tools;

public class MissingRequiredOptionsException extends CommandLineSyntaxException {
	private static final long serialVersionUID = 1L;

	private final Collection<Option> baseMissing;
	private final String command;
	private final Collection<Option> commandMissing;

	public MissingRequiredOptionsException(OptionScheme scheme, Collection<Option> baseMissing, String command,
		Collection<Option> commandMissing) {
		super(scheme, null, null);
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