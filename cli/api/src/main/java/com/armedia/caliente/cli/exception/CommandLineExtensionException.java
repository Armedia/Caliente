package com.armedia.caliente.cli.exception;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.token.Token;
import com.armedia.commons.utilities.Tools;

public class CommandLineExtensionException extends CommandLineSyntaxException {
	private static final long serialVersionUID = 1L;

	private static final String NO_DETAILS = "(no details given)";

	private final int pass;
	private final OptionValues baseValues;
	private final String command;
	private final OptionValues commandValues;
	private final String details;

	public CommandLineExtensionException(int pass, OptionValues baseValues, String command, OptionValues commandValues,
		Token token, String details) {
		super(null, null, token);
		this.pass = pass;
		this.baseValues = baseValues;
		this.command = command;
		this.commandValues = commandValues;
		this.details = (StringUtils.isBlank(details) ? CommandLineExtensionException.NO_DETAILS : details);
	}

	public final int getPass() {
		return this.pass;
	}

	public final OptionValues getBaseValues() {
		return this.baseValues;
	}

	public final String getCommand() {
		return this.command;
	}

	public final OptionValues getCommandValues() {
		return this.commandValues;
	}

	public final String getDetails() {
		return this.details;
	}

	@Override
	protected String renderMessage() {
		String commandStr = " (no command)";
		if (this.command != null) {
			commandStr = String.format(" (command=[%s])", this.command);
		}
		return String.format("Failed to extend the parameter schema on token %s on pass #%d%s: %s", getToken(),
			this.pass, commandStr, Tools.coalesce(this.details, this.details));
	}
}