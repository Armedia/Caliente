package com.armedia.caliente.cli;

import java.util.List;

public class CommandResultImpl extends ParameterResultImpl implements CommandResult {

	private final String command;
	private final ParameterValues commandValues;

	public CommandResultImpl(ParameterValues parameterValues, String command, ParameterValues commandValues,
		List<String> positionals) {
		super(parameterValues, positionals);
		this.command = command;
		this.commandValues = commandValues;
	}

	@Override
	public String getCommand() {
		return this.command;
	}

	@Override
	public ParameterValues getCommandValues() {
		return this.commandValues;
	}

}