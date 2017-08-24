package com.armedia.caliente.cli;

import java.util.List;

public class CommandSchemeResultImpl extends ParameterSchemeResultImpl implements CommandSchemeResult {

	private final Command command;
	private final ParameterValues commandValues;

	public CommandSchemeResultImpl(ParameterValues parameterValues, Command command, ParameterValues commandValues,
		List<String> positionals) {
		super(parameterValues, positionals);
		this.command = command;
		this.commandValues = commandValues;
	}

	@Override
	public Command getCommand() {
		return this.command;
	}

	@Override
	public ParameterValues getCommandValues() {
		return this.commandValues;
	}

}