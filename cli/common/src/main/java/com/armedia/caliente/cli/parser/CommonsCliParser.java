package com.armedia.caliente.cli.parser;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Option.Builder;
import org.apache.commons.cli.Options;

public class CommonsCliParser extends CommandLineParser {

	private class State {
		final Options options = new Options();
		final Map<String, CommandLineParameter> commandLineParameters = new HashMap<>();
		final String exe;

		private State(String executableName) {
			this.exe = executableName;
		}
	}

	private static final String STATE = "state";

	private State getState(Context ctx) {
		State state = ctx.getState(CommonsCliParser.STATE, State.class);
		if (state == null) { throw new IllegalStateException(
			String.format("Failed to find the required state field [%s]", CommonsCliParser.STATE)); }
		return state;
	}

	@Override
	protected void init(Context ctx, String executableName, Collection<CommandLineParameter> def) throws Exception {
		final State state = new State(executableName);
		for (CommandLineParameter p : def) {
			Option o = CommonsCliParser.buildOption(p.getDefinition());
			state.options.addOption(o);
			String key = CommonsCliParser.calculateKey(o);
			CommandLineParameter old = state.commandLineParameters.put(key, p);
			if (old != null) { throw new Exception(
				String.format("Duplicate parameter definition for option [%s]", key)); }
		}
		ctx.setState(CommonsCliParser.STATE, state);
	}

	@Override
	protected void parse(Context ctx, String... args) throws Exception {
		final State state = getState(ctx);
		org.apache.commons.cli.CommandLineParser parser = new DefaultParser();
		final org.apache.commons.cli.CommandLine cli = parser.parse(state.options, args);

		for (Option o : cli.getOptions()) {
			String key = CommonsCliParser.calculateKey(o);
			CommandLineParameter p = state.commandLineParameters.get(key);
			if (p == null) { throw new Exception(
				String.format("Failed to locate a parameter for option [%s] which should have been there", key)); }
			ctx.setParameter(p, o.getValuesList());
		}

		ctx.addRemainingParameters(cli.getArgList());
	}

	@Override
	protected String getHelpMessage(Context ctx, Throwable t) {
		final State state = getState(ctx);
		StringWriter w = new StringWriter();
		PrintWriter pw = new PrintWriter(w);
		HelpFormatter hf = new HelpFormatter();
		String footer = null;
		if (t != null) {
			footer = String.format("%nERROR: %s%n%n", t.getMessage());
		}
		hf.printHelp(pw, hf.getWidth(), state.exe,
			String.format("%nAvailable Parameters:%n------------------------------%n"), state.options,
			hf.getLeftPadding(), hf.getDescPadding(), footer, true);
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

	static String calculateKey(Option o) {
		if (o == null) { throw new IllegalArgumentException("Must provide an option whose key to calculate"); }
		return BaseParameterDefinition.calculateKey(o.getLongOpt(), o.getOpt());
	}
}