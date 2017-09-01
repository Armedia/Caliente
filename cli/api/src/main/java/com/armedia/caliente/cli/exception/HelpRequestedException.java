package com.armedia.caliente.cli.exception;

import com.armedia.caliente.cli.Command;
import com.armedia.caliente.cli.OptionScheme;

public class HelpRequestedException extends CommandLineException {
	private static final long serialVersionUID = 1L;

	private final OptionScheme baseScheme;
	private final Command command;

	public HelpRequestedException(OptionScheme baseScheme) {
		this(baseScheme, null, null);
	}

	public HelpRequestedException(OptionScheme baseScheme, Command command) {
		this(baseScheme, command, null);
	}

	public HelpRequestedException(OptionScheme baseScheme, CommandLineSyntaxException error) {
		this(baseScheme, null, error);
	}

	public HelpRequestedException(OptionScheme baseScheme, Command command, CommandLineSyntaxException error) {
		super(error);
		this.baseScheme = baseScheme;
		this.command = command;
	}

	@Override
	public CommandLineSyntaxException getCause() {
		return CommandLineSyntaxException.class.cast(super.getCause());
	}

	public OptionScheme getBaseScheme() {
		return this.baseScheme;
	}

	public Command getCommand() {
		return this.command;
	}
}