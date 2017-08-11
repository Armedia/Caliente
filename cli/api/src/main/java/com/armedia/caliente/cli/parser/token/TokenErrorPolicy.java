package com.armedia.caliente.cli.parser.token;

import java.util.List;

import com.armedia.caliente.cli.parser.Parameter;

public interface TokenErrorPolicy {

	public boolean isErrorMissingValues(Token token, Parameter parameter, List<String> values);

	public boolean isErrorTooManyValues(Token token, Parameter parameter, List<String> values);

	public boolean isErrorUnknownParameterFound(Token token);

	public boolean isErrorUnknownSubCommandFound(Token token);
}