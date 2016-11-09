package com.armedia.caliente.cli.parser;

import java.util.Set;

public interface CommandLineParser {

	public void init(Set<Parameter> def) throws Exception;

	public void parse(CommandLineParserListener listener, String... parameters) throws Exception;

	public String getHelpMessage(String executableName, Throwable thrown);

	public void cleanup();
}