package com.armedia.caliente.cli.exception;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionScheme;

public class InsufficientOptionValuesException extends CommandLineSyntaxException {
	private static final long serialVersionUID = 1L;

	public InsufficientOptionValuesException(OptionScheme optionScheme, Option option) {
		super(optionScheme, option, null);
	}

	@Override
	protected String renderMessage() {
		Option o = getOption();
		String longOpt = o.getLongOpt();
		Character shortOpt = o.getShortOpt();
		String label = "";
		if ((longOpt != null) && (shortOpt != null)) {
			label = String.format("-%s/--%s", shortOpt, longOpt);
		} else if (longOpt != null) {
			label = String.format("--%s", longOpt);
		} else {
			label = String.format("-%s", shortOpt);
		}
		return String.format("The option %s requires at least %d values", label, o.getMinArguments());
	}
}