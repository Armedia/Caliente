package com.armedia.caliente.cli.exception;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionScheme;

public class InsufficientOptionValuesException extends CommandLineSyntaxException {
	private static final long serialVersionUID = 1L;

	public InsufficientOptionValuesException(OptionScheme optionScheme, Option option) {
		super(optionScheme, option, null);
	}
}