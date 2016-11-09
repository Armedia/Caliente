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

public class CommonsCliParser implements CommandLineParser {

	private final Map<String, Parameter> parameters = new HashMap<>();
	private Options options = null;

	@Override
	public void init(Set<Parameter> def) throws Exception {
		this.options = new Options();
		this.parameters.clear();
		for (Parameter p : def) {
			Option o = CommonsCliParser.buildOption(p);
			this.options.addOption(o);
			this.parameters.put(o.getOpt(), p);
		}
	}

	@Override
	public void parse(CommandLineParserListener listener, String... args) throws Exception {

		org.apache.commons.cli.CommandLineParser parser = new DefaultParser();
		final org.apache.commons.cli.CommandLine cli = parser.parse(this.options, args);

		for (Option o : cli.getOptions()) {
			Parameter p = this.parameters.get(o.getOpt());
			if (p == null) {
				// WTF!?
			}
			listener.setParameter(p, o.getValuesList());
		}

		listener.addRemainingParameters(cli.getArgList());
	}

	@Override
	public String getHelpMessage(String executableName, Throwable t) {
		StringWriter w = new StringWriter();
		PrintWriter pw = new PrintWriter(w);
		HelpFormatter hf = new HelpFormatter();
		String footer = null;
		if (t != null) {
			footer = String.format("%nERROR: %s%n%n", t.getMessage());
		}
		hf.printHelp(pw, hf.getWidth(), executableName,
			String.format("%nAvailable Parameters:%n------------------------------%n"), this.options,
			hf.getLeftPadding(), hf.getDescPadding(), footer, true);
		w.flush();
		return w.toString();
	}

	@Override
	public void cleanup() {
		this.options = null;
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