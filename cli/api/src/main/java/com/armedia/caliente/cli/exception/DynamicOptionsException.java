package com.armedia.caliente.cli.exception;

import com.armedia.caliente.cli.OptionScheme;

public class DynamicOptionsException extends CommandLineSyntaxException {
	private static final long serialVersionUID = 1L;

	private final String message;

	public DynamicOptionsException(OptionScheme optionScheme, String message) {
		super(optionScheme, null, null);
		this.message = message;
	}

	@Override
	protected String renderMessage() {
		return this.message;
	}
}