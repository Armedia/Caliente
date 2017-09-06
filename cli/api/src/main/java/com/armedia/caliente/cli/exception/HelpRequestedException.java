package com.armedia.caliente.cli.exception;

import com.armedia.caliente.cli.Command;
import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionScheme;

public class HelpRequestedException extends CommandLineException {
	private static final long serialVersionUID = 1L;

	private final Option helpOption;
	private final OptionScheme baseScheme;
	private final Command command;

	public HelpRequestedException(Option helpOption, OptionScheme baseScheme) {
		this(helpOption, baseScheme, null, null);
	}

	public HelpRequestedException(Option helpOption, OptionScheme baseScheme, Command command) {
		this(helpOption, baseScheme, command, null);
	}

	public HelpRequestedException(Option helpOption, OptionScheme baseScheme, CommandLineSyntaxException error) {
		this(helpOption, baseScheme, null, error);
	}

	public HelpRequestedException(Option helpOption, OptionScheme baseScheme, Command command,
		CommandLineSyntaxException error) {
		super(error);
		this.helpOption = helpOption;
		this.baseScheme = baseScheme;
		this.command = command;
	}

	@Override
	public CommandLineSyntaxException getCause() {
		return CommandLineSyntaxException.class.cast(super.getCause());
	}

	public Option getHelpOption() {
		return this.helpOption;
	}

	public OptionScheme getBaseScheme() {
		return this.baseScheme;
	}

	public Command getCommand() {
		return this.command;
	}
}