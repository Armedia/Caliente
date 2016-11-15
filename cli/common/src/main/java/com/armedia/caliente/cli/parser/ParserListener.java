package com.armedia.caliente.cli.parser;

import java.util.List;

public interface ParserListener {

	public void parameterFound(Parameter parameter, List<String> arguments);

	public void terminatorFound();

	public void subCommandFound(String subCommand);

	public void extraArguments(List<String> arguments);

	public boolean errorMissingArguments(Parameter parameter);

	public boolean errorUnknownParameter(String value);

	public boolean errorOrphanedValue(String value);
}