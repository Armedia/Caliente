package com.armedia.cmf.validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Option.Builder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.armedia.commons.utilities.Tools;

public enum CLIParam {
	//
	help(0, "This help message"),
	debug(0, "Enable debugging"),
	bulk_import(1, true, "The location of the Bulk Import source data"),
	bulk_export(1, true, "The location of the Bulk Export validation data"),
	//
	;

	public final Option option;
	private final int paramCount;

	private CLIParam(int paramCount, boolean required, String description) {
		String longOpt = name().replace('_', '-');
		Builder b = Option.builder();
		if (required) {
			b.required();
		}
		b.longOpt(longOpt);
		b.desc(description);
		b.valueSeparator(',');
		if (paramCount < 0) {
			b.hasArgs();
		} else if (paramCount > 0) {
			b.numberOfArgs(paramCount);
		}
		this.option = b.build();
		this.paramCount = paramCount;
	}

	private CLIParam(int paramCount, String description) {
		this(paramCount, false, description);
	}

	public boolean isPresent() {
		return CLIParam.isPresent(this);
	}

	public Boolean getBoolean() {
		String s = getString();
		return (s != null ? Tools.toBoolean(s) : null);
	}

	public boolean getBoolean(boolean def) {
		Boolean v = getBoolean();
		return (v != null ? v.booleanValue() : def);
	}

	public List<Boolean> getAllBoolean() {
		List<String> l = getAllString();
		if ((l == null) || l.isEmpty()) { return Collections.emptyList(); }
		List<Boolean> r = new ArrayList<Boolean>(l.size());
		for (String s : l) {
			r.add(Tools.toBoolean(s));
		}
		return Tools.freezeList(r);
	}

	public Integer getInteger() {
		String s = getString();
		return (s != null ? Integer.valueOf(s) : null);
	}

	public int getInteger(int def) {
		Integer v = getInteger();
		return (v != null ? v.intValue() : def);
	}

	public List<Integer> getAllInteger() {
		List<String> l = getAllString();
		if ((l == null) || l.isEmpty()) { return Collections.emptyList(); }
		List<Integer> r = new ArrayList<Integer>(l.size());
		for (String s : l) {
			r.add(Integer.valueOf(s));
		}
		return Tools.freezeList(r);
	}

	public Double getDouble() {
		String s = getString();
		return (s != null ? Double.valueOf(s) : null);
	}

	public double getDouble(double def) {
		Double v = getDouble();
		return (v != null ? v.doubleValue() : def);
	}

	public List<Double> getAllDouble() {
		List<String> l = getAllString();
		if ((l == null) || l.isEmpty()) { return Collections.emptyList(); }
		List<Double> r = new ArrayList<Double>(l.size());
		for (String s : l) {
			r.add(Double.valueOf(s));
		}
		return Tools.freezeList(r);
	}

	public String getString() {
		return CLIParam.getString(this);
	}

	public String getString(String def) {
		final String v = getString();
		return (v != null ? v : def);
	}

	public List<String> getAllString() {
		return CLIParam.getAllString(this);
	}

	private static final String[] NO_OPTS = new String[0];
	private static final Map<CLIParam, List<String>> NO_PARSED = Collections.emptyMap();
	private static final List<String> NO_REMAINING = Collections.emptyList();
	private static AtomicReference<Map<CLIParam, List<String>>> CLI_PARSED = new AtomicReference<Map<CLIParam, List<String>>>(
		CLIParam.NO_PARSED);
	private static AtomicReference<List<String>> CLI_REMAINING = new AtomicReference<List<String>>(
		CLIParam.NO_REMAINING);

	public static String getString(CLIParam param) {
		if (param == null) { throw new IllegalArgumentException("Must provide a parameter to search for"); }
		Map<CLIParam, List<String>> m = CLIParam.getParsed();
		if (m == null) { return null; }
		List<String> l = m.get(param);
		if ((l == null) || l.isEmpty()) { return null; }
		return l.get(0);
	}

	public static List<String> getAllString(CLIParam param) {
		if (param == null) { throw new IllegalArgumentException("Must provide a parameter to search for"); }
		Map<CLIParam, List<String>> m = CLIParam.getParsed();
		if (m == null) { return null; }
		List<String> l = m.get(param);
		if ((l == null) || l.isEmpty()) { return CLIParam.NO_REMAINING; }
		return l;
	}

	public static boolean isPresent(CLIParam param) {
		if (param == null) { throw new IllegalArgumentException("Must provide a parameter to search for"); }
		Map<CLIParam, List<String>> m = CLIParam.getParsed();
		if (m == null) { return false; }
		return m.containsKey(param);
	}

	public static Map<CLIParam, List<String>> getParsed() {
		return CLIParam.CLI_PARSED.get();
	}

	public static List<String> getRemaining() {
		return CLIParam.CLI_REMAINING.get();
	}

	public static synchronized boolean parse(String... args) {
		if (args == null) {
			args = CLIParam.NO_OPTS;
		}
		// To start off, parse the command line
		Options options = new Options();
		for (CLIParam p : CLIParam.values()) {
			options.addOption(p.option);
		}

		CommandLineParser parser = new DefaultParser();
		final CommandLine cli;
		try {
			cli = parser.parse(options, args);
		} catch (ParseException e) {
			new HelpFormatter().printHelp("CMSMF",
				String.format("%nAvailable Parameters:%n------------------------------%n"), options,
				String.format("%nERROR: %s%n%n", e.getMessage()), true);
			return false;
		}

		if (cli.hasOption(CLIParam.help.option.getLongOpt())) {
			new HelpFormatter().printHelp("CMSMF",
				String.format("%nAvailable Parameters:%n------------------------------%n"), options, null, true);
			return false;
		}

		// Convert the command-line parameters into "configuration properties"
		Map<CLIParam, List<String>> cliParams = new EnumMap<CLIParam, List<String>>(CLIParam.class);
		for (CLIParam p : CLIParam.values()) {
			if (cli.hasOption(p.option.getLongOpt())) {
				// If it takes no parameters, ignore whatever was submitted
				if (p.paramCount == 0) {
					cliParams.put(p, CLIParam.NO_REMAINING);
					continue;
				}

				// It takes parameters, so ... store them
				String[] v = cli.getOptionValues(p.option.getLongOpt());
				if ((v != null) && (v.length > 0)) {
					List<String> l = null;
					// If it only has one, or it only takes one, only keep one
					if ((v.length == 1) || (p.paramCount == 1)) {
						// Single value, life is easy :)
						l = Collections.singletonList(v[0]);
					} else {
						l = Arrays.asList(v);
					}
					cliParams.put(p, Tools.freezeList(l));
				} else {
					// The parameters may be optional....???
					cliParams.put(p, CLIParam.NO_REMAINING);
				}
			}
		}
		List<?> remaining = cli.getArgList();
		if (!remaining.isEmpty()) {
			List<String> l = new ArrayList<String>(remaining.size());
			for (Object o : remaining) {
				l.add(Tools.toString(o));
			}
			CLIParam.CLI_REMAINING.set(Tools.freezeList(l));
		}
		CLIParam.CLI_PARSED.set(Tools.freezeMap(cliParams));
		return true;
	}
}