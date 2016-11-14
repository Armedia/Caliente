package com.armedia.caliente.cli.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;

class Argparse4jState extends CommandLineParserContext {
	final Map<String, CommandLineParameter> commandLineParameters = new HashMap<>();
	final String exe;
	final ArgumentParser parser;

	Argparse4jState(CommandLine cl, String executableName) {
		super(cl);
		this.exe = executableName;
		ArgumentParsers.setTerminalWidthDetection(true);
		this.parser = ArgumentParsers.newArgumentParser(executableName, false, ArgumentParsers.DEFAULT_PREFIX_CHARS,
			"@");
	}
}

public class Argparse4jParser extends CommandLineParser<Argparse4jState> {

	public Argparse4jParser() {
		throw new RuntimeException("Not supported yet");
	}

	@Override
	protected Argparse4jState createContext(CommandLine cl, String executableName, Collection<CommandLineParameter> def)
		throws Exception {
		return new Argparse4jState(cl, executableName);
	}

	@Override
	protected void parse(Argparse4jState ctx, String... parameters) throws Exception {
		ctx.parser.addArgument(parameters);
		List<String> unknown = new ArrayList<>(parameters.length);
		Namespace ns = ctx.parser.parseKnownArgs(parameters, unknown);
		for (String s : ns.getAttrs().keySet()) {
			List<String> v = ns.getList(s);
			// Match the key to the original parameter
			CommandLineParameter p = ctx.cl.getParameterByKey(s);
			ctx.setParameter(p, v);
		}
	}

	@Override
	protected String getHelpMessage(Argparse4jState ctx, Throwable thrown) {
		return null;
	}

	@Override
	protected void cleanup(Argparse4jState ctx) {
	}
}