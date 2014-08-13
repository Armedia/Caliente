package com.delta.cmsmf.mainEngine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang.SystemUtils;

import com.delta.cmsmf.constants.CMSMFProperties;
import com.delta.cmsmf.utils.ProcessFuture;

public class Launcher {

	static enum CLIParam {
		//
		help(null, false, "This help message"),
		test(null, false, "Enable test mode"),
		cfg(null, true, "The configuration file to use"),
		dfc(null, true, "The path where DFC is installed (i.e. instead of DOCUMENTUM_SHARED)"),
		// dctm(null, true, "The user's local Documentum path (i.e. instead of DOCUMENTUM)"),
		mode(null, true, true, "The mode of operation, either 'encrypt', 'import' or 'export'"),
		docbase(null, true, "The docbase name to connect to"),
		user(null, true, "The username to connect with"),
		password(null, true, "The password to connect with"),
		predicate(CMSMFProperties.EXPORT_QUERY_PREDICATE, true, "The DQL Predicate to use for exporting"),
		buffer(CMSMFProperties.CONTENT_READ_BUFFER_SIZE, true, "The size of the read buffer"),
		streams(CMSMFProperties.STREAMS_DIRECTORY, true, "The Streams directory to use"),
		content(CMSMFProperties.CONTENT_DIRECTORY, true, "The Content directory to use"),
		compress(CMSMFProperties.COMPRESSDATA_FLAG, false, "Enable compression for the data exported (GZip)"),
		attributes(CMSMFProperties.OWNER_ATTRIBUTES, true, "The attributes to check for"),
		errorCount(CMSMFProperties.IMPORT_MAX_ERRORS, true, "The number of errors to accept before aborting an import"),
		defaultPassword(CMSMFProperties.DEFAULT_USER_PASSWORD, true,
			"The default password to use for users being copied over (leave blank to use the same login name)");

		public final CMSMFProperties property;
		public final Option option;

		private CLIParam(CMSMFProperties property, boolean hasParameter, boolean required, String description) {
			this.property = property;
			this.option = new Option(null, name().replace('_', '-'), hasParameter, description);
			this.option.setRequired(required);
		}

		private CLIParam(CMSMFProperties property, boolean hasParameter, String description) {
			this(property, hasParameter, false, description);
		}
	}

	private static final String ENV_DOCUMENTUM_SHARED = "DOCUMENTUM_SHARED";

	// private static final String ENV_DOCUMENTUM = "DOCUMENTUM";

	private static final String DCTM_JAR = "dctm.jar";

	private static final String MAIN_CLASS = "com.delta.cmsmf.mainEngine.CMSMFMain_%s";

	private static final Class<?>[] PARAMETERS = new Class[] {
		URL.class
	};

	private static final URLClassLoader CL;
	private static final Method METHOD;

	static {
		ClassLoader cl = ClassLoader.getSystemClassLoader();
		if (!(cl instanceof URLClassLoader)) { throw new RuntimeException("System Classloader is not a URLClassLoader"); }
		CL = URLClassLoader.class.cast(cl);
		try {
			METHOD = URLClassLoader.class.getDeclaredMethod("addURL", Launcher.PARAMETERS);
			Launcher.METHOD.setAccessible(true);
		} catch (Throwable t) {
			throw new RuntimeException("Failed to initialize access to the addURL() method in the system classloader",
				t);
		}
	}

	private static void addToClassPath(URL u) throws IOException {
		try {
			Launcher.METHOD.invoke(Launcher.CL, u);
		} catch (Throwable t) {
			throw new IOException(String.format("Failed to add the URL [%s] to the system classloader", u), t);
		}
	}

	private static void addToClassPath(File f) throws IOException {
		Launcher.addToClassPath(f.toURI().toURL());
	}

	private static String[] CLI_ARGS = null;
	private static Map<CLIParam, String> CLI_PARSED = null;

	static final Map<CLIParam, String> getParsedCliArgs() {
		return Launcher.CLI_PARSED;
	}

	static final String[] getCliArgs() {
		return Launcher.CLI_ARGS;
	}

	@SuppressWarnings("unused")
	private static ProcessFuture execClass(Class<?> klass, Collection<File> classpath, Map<String, String> environment,
		String... args) throws IOException, InterruptedException {

		try {
			Method m = klass.getMethod("main", Array.class);
			if (!Modifier.isStatic(m.getModifiers())) { return null; }
		} catch (SecurityException e) {
			// Can't tell, so we keep going...
		} catch (NoSuchMethodException e) {
			// No such method...
			return null;
		}

		// This will help identify
		RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
		List<String> jvmArgs = runtimeMxBean.getInputArguments();

		String javaHome = System.getProperty("java.home");
		File javaBin = new File(javaHome);
		javaBin = new File(javaBin, "bin");
		javaBin = new File(javaBin, (SystemUtils.IS_OS_WINDOWS ? "java.exe" : "java"));

		StringBuilder b = new StringBuilder();

		// First, parse out the current classpath
		String currentCp = System.getProperty("java.class.path");
		StringTokenizer tok = new StringTokenizer(currentCp, File.pathSeparator);
		while (tok.hasMoreElements()) {
			final String token = tok.nextToken();
			if (token.length() == 0) {
				continue;
			}
			if (b.length() > 0) {
				b.append(File.pathSeparatorChar);
			}
			b.append(token);
		}

		// Current classpath has been pre-pended, we add the additional items
		for (File f : classpath) {
			if (b.length() > 0) {
				b.append(File.pathSeparatorChar);
			}
			b.append(f.getAbsolutePath());
		}
		List<String> command = new ArrayList<String>();
		command.add(javaBin.getAbsolutePath());

		// Add the classpath first
		command.add("-cp");
		command.add(b.toString());

		for (String s : jvmArgs) {
			// Ignore -cp and -classpath
			if ("-classpath".equals(s) || "-cp".equals(s)) {
				continue;
			}
			command.add(s);
		}

		// Add the main class
		command.add(klass.getCanonicalName());

		// Add the class arguments
		for (String s : args) {
			if (s != null) {
				continue;
			}
			command.add(s);
		}

		// Build the process
		final ProcessBuilder builder = new ProcessBuilder(command);
		if ((environment != null) && !environment.isEmpty()) {
			builder.environment().putAll(environment);
		}
		return new ProcessFuture(builder.start());
	}

	public static void main(String[] args) throws Throwable {

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
			return;
		}

		if (cli.hasOption(CLIParam.help.option.getLongOpt())) {
			new HelpFormatter().printHelp("CMSMF",
				String.format("%nAvailable Parameters:%n------------------------------%n"), options, null, true);
			return;
		}

		// Convert the command-line parameters into "configuration properties"
		Map<CLIParam, String> cliParams = new EnumMap<CLIParam, String>(CLIParam.class);
		for (CLIParam p : CLIParam.values()) {
			if (cli.hasOption(p.option.getLongOpt())) {
				cliParams.put(p, cli.getOptionValue(p.option.getLongOpt()));
			}
		}
		Launcher.CLI_ARGS = args.clone();
		Launcher.CLI_PARSED = Collections.unmodifiableMap(cliParams);

		String var = null;
		File base = null;
		File tgt = null;

		// First, add the ${PWD}/cfg directory to the classpath - whether it exists or not
		var = System.getProperty("user.dir");
		base = new File(var);
		tgt = new File(var, "cfg");
		Launcher.addToClassPath(tgt);

		/*
		// Next, add ${DOCUMENTUM}/config to the classpath
		var = System.getenv(Launcher.ENV_DOCUMENTUM);
		if (cliParams.containsKey(CLIParam.dctm)) {
			// DFC is specified
			var = cliParams.get(CLIParam.dctm);
		} else {
			// Go with the environment
			if (var == null) { throw new RuntimeException(String.format("The environment variable [%s] is not set",
				Launcher.ENV_DOCUMENTUM)); }
		}

		base = new File(var).getCanonicalFile();
		if (!base.isDirectory()) { throw new FileNotFoundException(String.format("Could not find the directory [%s]",
			base.getAbsolutePath())); }

		System.out.printf("Using %s=[%s]%n", Launcher.ENV_DOCUMENTUM, base.getAbsolutePath());
		// Make sure the environment reflects our changes
		// System.getenv().put(Launcher.ENV_DOCUMENTUM, base.getAbsolutePath());

		tgt = new File(base, "config");
		if (!base.isDirectory()) { throw new FileNotFoundException(String.format("Could not find the directory [%s]",
			tgt.getAbsolutePath())); }

		Launcher.addToClassPath(tgt);
		*/

		// Next, identify the DOCUMENTUM_SHARED location, and if dctm.jar is in there
		var = System.getenv(Launcher.ENV_DOCUMENTUM_SHARED);
		if (cliParams.containsKey(CLIParam.dfc)) {
			// DFC is specified
			var = cliParams.get(CLIParam.dfc);
		} else {
			// Go with the environment
			if (var == null) { throw new RuntimeException(String.format("The environment variable [%s] is not set",
				Launcher.ENV_DOCUMENTUM_SHARED)); }
		}

		// Next, is it a directory?
		base = new File(var).getCanonicalFile();
		if (!base.isDirectory()) { throw new FileNotFoundException(String.format("Could not find the directory [%s]",
			base.getAbsolutePath())); }

		System.out.printf("Using %s=[%s]%n", Launcher.ENV_DOCUMENTUM_SHARED, base.getAbsolutePath());

		// Make sure the environment reflects our changes
		// System.getenv().put(Launcher.ENV_DOCUMENTUM_SHARED, base.getAbsolutePath());

		// Next, does dctm.jar exist in there?
		tgt = new File(base, Launcher.DCTM_JAR);
		if (!tgt.isFile()) { throw new FileNotFoundException(String.format("Could not find the JAR file [%s]",
			tgt.getAbsolutePath())); }

		// Next, to the classpath
		Launcher.addToClassPath(tgt);

		// Finally, launch the main class
		// We launch like this because we have to patch the classpath before we link into the rest
		// of the code. If we don't do it like this, the app will refuse to launch altogether
		Class.forName(String.format(Launcher.MAIN_CLASS, cliParams.get(CLIParam.mode))).newInstance();
	}
}