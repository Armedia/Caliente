package com.armedia.caliente.cli.exception;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.token.Token;

public class TooManyOptionValuesException extends CommandLineSyntaxException {
	private static final long serialVersionUID = 1L;

	public TooManyOptionValuesException(OptionScheme scheme, Option option, Token token) {
		super(scheme, option, token);
	}

	@Override
	protected String renderMessage() {
		int min = getOption().getMinValueCount();
		int max = getOption().getMaxValueCount();

		String msg = "";
		if (min == max) {
			if (max == 0) {
				msg = "no";
			} else {
				msg = String.format("exactly %d", max);
			}
		} else {
			msg = String.format("at most %d", max);
		}

		Option o = getOption();
		String longOpt = o.getLongOpt();
		Character shortOpt = o.getShortOpt();
		String option = "";
		if ((longOpt != null) && (shortOpt != null)) {
			option = String.format("-%s/--%s", shortOpt, longOpt);
		} else if (longOpt != null) {
			option = String.format("--%s", longOpt);
		} else {
			option = String.format("-%s", shortOpt);
		}

		return String.format("Too many values given for the option %s - %s positional arguments are allowed", option,
			msg);
	}
}