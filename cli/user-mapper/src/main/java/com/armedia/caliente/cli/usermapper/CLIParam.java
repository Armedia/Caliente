package com.armedia.caliente.cli.usermapper;

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
	lib(
		1,
		"The directory which contains extra classes (JARs, ZIPs or a classes directory) that should be added to the classpath"),
	dfc_prop(1, "The dfc.properties file to use instead of the default"),
	docbase(1, true, "The Documentum repostory name to connect to"),
	dfc(1, "The path where DFC is installed (i.e. instead of DOCUMENTUM_SHARED)"),
	dctm(1, "The user's local Documentum path (i.e. instead of DOCUMENTUM)"),
	dctm_user(1, true, "The username to connect to Documentum with"),
	dctm_pass(1, "The password to connect to Documentum with"),
	dctm_sam(
		-1,
		"The Documentum attribute to attempt to use for mapping directly to the sAMAccountName. Multiple instances of this parameter may be specified and each will be tried in turn.  The default mode is to first try user_login_name, then try user_os_name"),
	ldap_url(1, "The LDAP URL to bind to"),
	ldap_binddn(1, "The DN to bind to LDAP with"),
	ldap_basedn(1, "The Base DN to search LDAP for both users and groups (SUB scope)"),
	ldap_user_basedn(1, "The Base DN to search LDAP for users (SUB scope)"),
	ldap_group_basedn(1, "The Base DN to search LDAP for groups (SUB scope)"),
	ldap_pass(1, "The password to bind to LDAP with"),
	ldap_on_demand(0, "Execute LDAP queries on demand vs. batched up front"),
	add_docbase(
		0,
		"Add the docbase name to the files generated (use for running multiple instances at once in the same directory)"),
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

	public List<String> getAllString(List<String> def) {
		List<String> v = CLIParam.getAllString(this);
		if (v == null) { return def; }
		return v;
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
		if (l != null) { return l; }
		if (m.containsKey(param)) { return CLIParam.NO_REMAINING; }
		return null;
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