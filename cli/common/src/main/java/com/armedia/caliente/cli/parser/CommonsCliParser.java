package com.armedia.caliente.cli.parser;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Option.Builder;
import org.apache.commons.cli.Options;

public class CommonsCliParser extends CommandLineParser {

	private static final String PARAMETERS = "parameters";
	private static final String OPTIONS = "options";

	@Override
	protected void init(Context ctx, Set<Parameter> def) throws Exception {
		Options options = new Options();
		Map<String, Parameter> parameters = new HashMap<>();
		for (Parameter p : def) {
			Option o = CommonsCliParser.buildOption(p.getDefinition());
			options.addOption(o);
			Parameter old = parameters.put(o.getOpt(), p);
			if (old != null) { throw new Exception(String.format(
				"Duplicate parameter definition for option [%s]: %s and %s", o.getOpt(), old.getKey(), p.getKey())); }
		}
		ctx.setState(CommonsCliParser.PARAMETERS, parameters);
		ctx.setState(CommonsCliParser.OPTIONS, options);
	}

	@Override
	protected void parse(Context ctx, String... args) throws Exception {
		@SuppressWarnings("unchecked")
		Map<String, Parameter> parameters = ctx.getState(CommonsCliParser.PARAMETERS, Map.class);
		if (parameters == null) { throw new IllegalStateException(
			String.format("Failed to find the required state field [%s]", CommonsCliParser.PARAMETERS)); }
		Options options = ctx.getState(CommonsCliParser.OPTIONS, Options.class);
		if (options == null) { throw new IllegalStateException(
			String.format("Failed to find the required state field [%s]", CommonsCliParser.OPTIONS)); }

		org.apache.commons.cli.CommandLineParser parser = new DefaultParser();
		final org.apache.commons.cli.CommandLine cli = parser.parse(options, args);

		for (Option o : cli.getOptions()) {
			Parameter p = parameters.get(o.getOpt());
			if (p == null) { throw new Exception(String
				.format("Failed to locate a parameter for option [%s] which should have been there", o.getOpt())); }
			ctx.setParameter(p, o.getValuesList());
		}

		ctx.addRemainingParameters(cli.getArgList());
	}

	@Override
	protected String getHelpMessage(Context ctx, String executableName, Throwable t) {
		Options options = ctx.getState(CommonsCliParser.OPTIONS, Options.class);
		if (options == null) { throw new IllegalStateException(
			String.format("Failed to find the required state field [%s]", CommonsCliParser.OPTIONS)); }
		StringWriter w = new StringWriter();
		PrintWriter pw = new PrintWriter(w);
		HelpFormatter hf = new HelpFormatter();
		String footer = null;
		if (t != null) {
			footer = String.format("%nERROR: %s%n%n", t.getMessage());
		}
		hf.printHelp(pw, hf.getWidth(), executableName,
			String.format("%nAvailable Parameters:%n------------------------------%n"), options, hf.getLeftPadding(),
			hf.getDescPadding(), footer, true);
		w.flush();
		return w.toString();
	}

	private static Option buildOption(ParameterDefinition def) {
		Builder b = (def.getShortOpt() == null ? Option.builder() : Option.builder(def.getShortOpt().toString()));
		b.required(def.isRequired());
		if (def.getLongOpt() != null) {
			b.longOpt(def.getLongOpt());
		}
		if (def.getDescription() != null) {
			b.desc(def.getDescription());
		}
		if (def.getValueSep() != null) {
			b.valueSeparator(def.getValueSep());
		}
		final int paramCount = def.getValueCount();
		if (paramCount != 0) {
			if (def.getValueName() != null) {
				b.argName(def.getValueName());
			}
			b.optionalArg(def.isValueOptional());
			if (paramCount < 0) {
				b.hasArgs();
			}
			if (paramCount > 0) {
				b.numberOfArgs(paramCount);
			}
		}
		return b.build();
	}
}