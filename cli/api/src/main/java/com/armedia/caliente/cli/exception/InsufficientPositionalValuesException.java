package com.armedia.caliente.cli.exception;

import com.armedia.caliente.cli.OptionScheme;

public class InsufficientPositionalValuesException extends CommandLineSyntaxException {
	private static final long serialVersionUID = 1L;

	public InsufficientPositionalValuesException(OptionScheme scheme) {
		super(scheme, null, null);
	}

	@Override
	protected String renderMessage() {
		return String.format("At least %d positional values are required", getOptionScheme().getMinArgs());
	}
}