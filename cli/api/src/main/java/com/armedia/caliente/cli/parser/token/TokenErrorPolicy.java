package com.armedia.caliente.cli.parser.token;

import java.util.List;

import com.armedia.caliente.cli.ParameterDefinition;
import com.armedia.caliente.cli.token.Token;

public interface TokenErrorPolicy {

	public boolean isErrorMissingValues(Token token, ParameterDefinition parameterDefinition, List<String> values);

	public boolean isErrorTooManyValues(Token token, ParameterDefinition parameterDefinition, List<String> values);

	public boolean isErrorUnknownParameterFound(Token token);

	public boolean isErrorUnknownSubCommandFound(Token token);
}