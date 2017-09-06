package com.armedia.caliente.cli.exception;

import java.util.Set;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionScheme;
import com.armedia.commons.utilities.Tools;

public class IllegalOptionValuesException extends CommandLineSyntaxException {
	private static final long serialVersionUID = 1L;

	private final Set<String> values;

	public IllegalOptionValuesException(OptionScheme optionScheme, Option option, Set<String> values) {
		super(optionScheme, option, null);
		this.values = Tools.freezeCopy(values);
	}

	public Set<String> getValues() {
		return this.values;
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
		return String.format("The values %s are not valid for the option %s", this.values, label);
	}
}