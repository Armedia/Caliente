package com.armedia.caliente.cli.parser;

import java.util.List;

public interface TokenErrorPolicy {

	public boolean isErrorMissingValues(Token token, Parameter parameter, List<String> values);

	public boolean isErrorTooManyValues(Token token, Parameter parameter, List<String> values);

	public boolean isErrorUnknownParameterFound(Token token);

	public boolean isErrorUnknownSubCommandFound(Token token);
}