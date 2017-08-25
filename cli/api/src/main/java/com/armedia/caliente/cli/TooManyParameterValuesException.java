package com.armedia.caliente.cli;

import com.armedia.caliente.cli.token.Token;
import com.armedia.caliente.cli.token.TokenSource;

public class TooManyParameterValuesException extends CommandLineSyntaxException {
	private static final long serialVersionUID = 1L;

	private final Parameter parameter;
	private final String string;

	public TooManyParameterValuesException(Parameter parameter, Token token) {
		this(parameter, token.getSource(), token.getIndex(), token.getRawString());
	}

	public TooManyParameterValuesException(Parameter parameter, TokenSource source, int index, String string) {
		super(source, index);
		this.string = string;
		this.parameter = parameter;
	}

	public final Parameter getParameter() {
		return this.parameter;
	}

	public final String getString() {
		return this.string;
	}
}