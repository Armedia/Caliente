package com.armedia.caliente.cli.exception;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.OptionValueFilter;
import com.armedia.commons.utilities.Tools;

public class IllegalOptionValuesException extends CommandLineSyntaxException {
	private static final long serialVersionUID = 1L;

	private final Set<String> values;
	private final String definition;

	public IllegalOptionValuesException(OptionScheme optionScheme, Option option, Set<String> values) {
		super(optionScheme, option, null);
		this.values = Tools.freezeCopy(values);
		OptionValueFilter filter = option.getValueFilter();
		String definition = null;
		if (filter != null) {
			definition = StringUtils.strip(filter.getDefinition());
		}
		if (!StringUtils.isBlank(definition)) {
			this.definition = String.format(", must be %s", option.getValueFilter().getDefinition());
		} else {
			this.definition = "";
		}
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
		final String plural = (this.values.size() == 1 ? "" : "s");
		final String is_are = (this.values.size() == 1 ? "is" : "are");
		if ((longOpt != null) && (shortOpt != null)) {
			label = String.format("-%s/--%s", shortOpt, longOpt);
		} else if (longOpt != null) {
			label = String.format("--%s", longOpt);
		} else {
			label = String.format("-%s", shortOpt);
		}
		return String.format("The value%s %s %s not valid for the option %s%s", plural, this.values, is_are, label,
			this.definition);
	}
}