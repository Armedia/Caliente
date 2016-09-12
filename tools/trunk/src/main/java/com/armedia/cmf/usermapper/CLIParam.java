package com.armedia.cmf.usermapper;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public enum CLIParam {
	//
	help(0, "This help message"),
	debug(0, "Enable debugging"),
	dfc_prop(1, "The dfc.properties file to use instead of the default"),
	docbase(1, true, "The Documentum repostory name to connect to"),
	dfc(1, "The path where DFC is installed (i.e. instead of DOCUMENTUM_SHARED)"),
	dctm(1, "The user's local Documentum path (i.e. instead of DOCUMENTUM)"),
	dctm_user(1, true, "The username to connect to Documentum with"),
	dctm_pass(1, true, "The password to connect to Documentum with"),
	ldap_url(1, "The LDAP URL to bind to"),
	ldap_binddn(1, "The DN to bind to LDAP with"),
	ldap_basedn(1, "The Base DN to search LDAP for both users and groups (SUB scope)"),
	ldap_user_basedn(1, "The Base DN to search LDAP for users (SUB scope)"),
	ldap_group_basedn(1, "The Base DN to search LDAP for groups (SUB scope)"),
	ldap_pass(1, "The password to bind to LDAP with"),
	ldap_on_demand(0, "Execute LDAP queries on demand vs. batched up front"),;

	public final Option option;

	private CLIParam(int paramCount, boolean required, String description) {
		if (required) {
			OptionBuilder.isRequired();
		}
		OptionBuilder.withLongOpt(name().replace('_', '-'));
		OptionBuilder.withDescription(description);
		OptionBuilder.withValueSeparator(',');
		if (paramCount < 0) {
			OptionBuilder.hasArgs(Integer.MAX_VALUE);
		} else if (paramCount > 0) {
			OptionBuilder.hasArgs(paramCount);
		}
		this.option = OptionBuilder.create();
	}

	private CLIParam(int paramCount, String description) {
		this(paramCount, false, description);
	}

	public boolean isPresent() {
		return CLIParam.isPresent(this);
	}

	public Boolean getBoolean() {
		String s = getString();
		return (s != null ? Boolean.valueOf(s) : null);
	}

	public boolean getBoolean(boolean def) {
		Boolean v = getBoolean();
		return (v != null ? v.booleanValue() : def);
	}

	public Integer getInteger() {
		String s = getString();
		return (s != null ? Integer.valueOf(s) : null);
	}

	public int getInteger(int def) {
		Integer v = getInteger();
		return (v != null ? v.intValue() : def);
	}

	public Double getDouble() {
		String s = getString();
		return (s != null ? Double.valueOf(s) : null);
	}

	public double getDouble(double def) {
		Double v = getDouble();
		return (v != null ? v.doubleValue() : def);
	}

	public String getString() {
		return CLIParam.getString(this);
	}

	public String getString(String def) {
		final String v = getString();
		return (v != null ? v : def);
	}

	private static final String[] NO_OPTS = new String[0];
	private static final Map<CLIParam, String> NO_PARSED = Collections.emptyMap();
	private static AtomicReference<Map<CLIParam, String>> CLI_PARSED = new AtomicReference<Map<CLIParam, String>>(
		CLIParam.NO_PARSED);

	public static String getString(CLIParam param) {
		if (param == null) { throw new IllegalArgumentException("Must provide a parameter to search for"); }
		Map<CLIParam, String> m = CLIParam.getParsed();
		if (m == null) { return null; }
		return m.get(param);
	}

	public static boolean isPresent(CLIParam param) {
		if (param == null) { throw new IllegalArgumentException("Must provide a parameter to search for"); }
		Map<CLIParam, String> m = CLIParam.getParsed();
		if (m == null) { return false; }
		return m.containsKey(param);
	}

	public static Map<CLIParam, String> getParsed() {
		return CLIParam.CLI_PARSED.get();
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

		CommandLineParser parser = new GnuParser();
		final CommandLine cli;
		try {
			cli = parser.parse(options, args);
		} catch (ParseException e) {
			new HelpFormatter().printHelp("Caliente Data Generator",
				String.format("%nAvailable Parameters:%n------------------------------%n"), options,
				String.format("%nERROR: %s%n%n", e.getMessage()), true);
			return false;
		}

		if (cli.hasOption(CLIParam.help.option.getLongOpt())) {
			new HelpFormatter().printHelp("Caliente Data Generator",
				String.format("%nAvailable Parameters:%n------------------------------%n"), options, null, true);
			return false;
		}

		// Convert the command-line parameters into "configuration properties"
		Map<CLIParam, String> cliParams = new EnumMap<CLIParam, String>(CLIParam.class);
		StringBuilder b = new StringBuilder();
		for (CLIParam p : CLIParam.values()) {
			if (cli.hasOption(p.option.getLongOpt())) {
				String[] v = cli.getOptionValues(p.option.getLongOpt());
				if ((v != null) && (v.length > 0)) {
					if (v.length == 1) {
						// Single value, life is easy :)
						cliParams.put(p, v[0]);
					} else {
						// Multi-value, must concatenate as comma-separated
						b.setLength(0);
						for (String s : v) {
							if (b.length() > 0) {
								b.append(',');
							}
							b.append(s);
						}
						cliParams.put(p, b.toString());
					}
				} else {
					cliParams.put(p, null);
				}
			}
		}
		CLIParam.CLI_PARSED.set(Collections.unmodifiableMap(cliParams));
		return true;
	}
}