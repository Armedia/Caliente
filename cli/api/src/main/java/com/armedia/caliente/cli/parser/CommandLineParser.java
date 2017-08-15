package com.armedia.caliente.cli.parser;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.Parameter;

public abstract class CommandLineParser<C extends CommandLineParserContext> {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	protected abstract C createContext(CommandLine cl, String executableName, Collection<? extends Parameter> def)
		throws Exception;

	protected abstract void parse(final C ctx, String... parameters) throws Exception;

	protected abstract String getHelpMessage(final C ctx, Throwable thrown);

	protected abstract void cleanup(C ctx);
}