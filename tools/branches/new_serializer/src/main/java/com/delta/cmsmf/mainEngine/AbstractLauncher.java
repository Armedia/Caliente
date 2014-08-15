package com.delta.cmsmf.mainEngine;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

abstract class AbstractLauncher {

	protected static String[] CLI_ARGS = null;
	protected static Map<CLIParam, String> CLI_PARSED = null;

	protected static Map<CLIParam, String> parseArguments(String... args) throws Throwable {
		// To start off, parse the command line
		Options options = new Options();
		for (CLIParam p : CLIParam.values()) {
			options.addOption(p.option);
		}

		CommandLineParser parser = new PosixParser();
		final CommandLine cli;
		try {
			cli = parser.parse(options, args);
		} catch (ParseException e) {
			new HelpFormatter().printHelp("CMSMF",
				String.format("%nAvailable Parameters:%n------------------------------%n"), options,
				String.format("%nERROR: %s%n%n", e.getMessage()), true);
			return null;
		}

		if (cli.hasOption(CLIParam.help.option.getLongOpt())) {
			new HelpFormatter().printHelp("CMSMF",
				String.format("%nAvailable Parameters:%n------------------------------%n"), options, null, true);
			return null;
		}

		// Convert the command-line parameters into "configuration properties"
		Map<CLIParam, String> cliParams = new EnumMap<CLIParam, String>(CLIParam.class);
		for (CLIParam p : CLIParam.values()) {
			if (cli.hasOption(p.option.getLongOpt())) {
				String value = cli.getOptionValue(p.option.getLongOpt());
				if (value == null) {
					value = Boolean.TRUE.toString();
				}
				cliParams.put(p, value);
			}
		}
		AbstractLauncher.CLI_ARGS = args.clone();
		AbstractLauncher.CLI_PARSED = Collections.unmodifiableMap(cliParams);
		return cliParams;
	}
}