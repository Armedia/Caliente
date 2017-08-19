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

import com.armedia.caliente.cli.Parameter;

class CommonsCliState extends CommandLineParserContext {
	final Options options = new Options();
	final Map<String, Parameter> commandLineParameters = new HashMap<>();
	final String exe;

	CommonsCliState(CommandLine cl, String executableName) {
		super(cl);
		this.exe = executableName;
	}
}

public class CommonsCliParser extends CommandLineParser<CommonsCliState> {

	@Override
	protected CommonsCliState createContext(CommandLine cl, String executableName, Collection<? extends Parameter> def)
		throws Exception {
		CommonsCliState ctx = new CommonsCliState(cl, executableName);
		for (Parameter p : def) {
			Option o = CommonsCliParser.buildOption(p);
			ctx.options.addOption(o);
			String key = CommonsCliParser.calculateKey(o);
			Parameter old = ctx.commandLineParameters.put(key, p);
			if (old != null) { throw new Exception(
				String.format("Duplicate parameter definition for option [%s]", key)); }
		}
		return ctx;
	}

	@Override
	protected void parse(CommonsCliState ctx, String... args) throws Exception {
		org.apache.commons.cli.CommandLineParser parser = new DefaultParser();
		final org.apache.commons.cli.CommandLine cli = parser.parse(ctx.options, args, true);

		for (Option o : cli.getOptions()) {
			String key = CommonsCliParser.calculateKey(o);
			Parameter p = ctx.commandLineParameters.get(key);
			if (p == null) { throw new Exception(
				String.format("Failed to locate a parameter for option [%s] which should have been there", key)); }
			ctx.setParameter(p, o.getValuesList());
		}

		ctx.addRemainingParameters(cli.getArgList());
	}

	@Override
	protected String getHelpMessage(CommonsCliState ctx, Throwable t) {
		StringWriter w = new StringWriter();
		PrintWriter pw = new PrintWriter(w);
		HelpFormatter hf = new HelpFormatter();
		String footer = null;
		if (t != null) {
			footer = String.format("%nERROR: %s%n%n", t.getMessage());
		}
		hf.printHelp(pw, hf.getWidth(), ctx.exe,
			String.format("%nAvailable Parameters:%n------------------------------%n"), ctx.options,
			hf.getLeftPadding(), hf.getDescPadding(), footer, true);
		w.flush();
		return w.toString();
	}

	private static Option buildOption(Parameter def) {
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
		final int paramCount = def.getMaxValueCount();
		if (paramCount != 0) {
			if (def.getValueName() != null) {
				b.argName(def.getValueName());
			}
			b.optionalArg(def.getMinValueCount() <= 0);
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
		return Parameter.calculateKey(o.getLongOpt(), o.getOpt());
	}

	@Override
	protected void cleanup(CommonsCliState ctx) {
		ctx.commandLineParameters.clear();
	}
}