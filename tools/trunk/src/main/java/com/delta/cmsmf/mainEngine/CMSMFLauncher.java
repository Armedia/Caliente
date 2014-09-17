package com.delta.cmsmf.mainEngine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.LogManager;
import org.apache.log4j.xml.DOMConfigurator;

import com.delta.cmsmf.utils.ClasspathPatcher;

public class CMSMFLauncher extends AbstractLauncher {

	private static final String ENV_DOCUMENTUM_SHARED = "DOCUMENTUM_SHARED";
	private static final String ENV_DOCUMENTUM = "DOCUMENTUM";
	private static final String DCTM_JAR = "dctm.jar";

	private static final String MAIN_CLASS = "com.delta.cmsmf.mainEngine.CMSMFMain_%s";

	private static Properties PARAMETER_PROPERTIES = new Properties();

	public static String getParameter(CLIParam parameter) {
		return AbstractLauncher.CLI_PARSED.get(parameter);
	}

	private static void patchClasspath(Map<CLIParam, String> cliParams) throws IOException {
		String var = null;
		File base = null;
		File tgt = null;

		// First, add the ${PWD}/cfg directory to the classpath - whether it exists or not
		var = System.getProperty("user.dir");
		base = new File(var);
		tgt = new File(var, "cfg");
		ClasspathPatcher.addToClassPath(base);
		ClasspathPatcher.addToClassPath(tgt);

		// Next, add ${DOCUMENTUM}/config to the classpath
		var = System.getenv(CMSMFLauncher.ENV_DOCUMENTUM);
		if (cliParams.containsKey(CLIParam.dctm)) {
			// DFC is specified
			var = cliParams.get(CLIParam.dctm);
		} else {
			// Go with the environment
			if (var == null) { throw new RuntimeException(String.format("The environment variable [%s] is not set",
				CMSMFLauncher.ENV_DOCUMENTUM)); }
		}

		base = new File(var).getCanonicalFile();
		if (!base.isDirectory()) { throw new FileNotFoundException(String.format("Could not find the directory [%s]",
			base.getAbsolutePath())); }

		tgt = new File(base, "config");
		if (!base.isDirectory()) { throw new FileNotFoundException(String.format("Could not find the directory [%s]",
			tgt.getAbsolutePath())); }

		ClasspathPatcher.addToClassPath(tgt);

		// Next, identify the DOCUMENTUM_SHARED location, and if dctm.jar is in there
		var = System.getenv(CMSMFLauncher.ENV_DOCUMENTUM_SHARED);
		if (cliParams.containsKey(CLIParam.dfc)) {
			// DFC is specified
			var = cliParams.get(CLIParam.dfc);
		} else {
			// Go with the environment
			if (var == null) { throw new RuntimeException(String.format("The environment variable [%s] is not set",
				CMSMFLauncher.ENV_DOCUMENTUM_SHARED)); }
		}

		// Next, is it a directory?
		base = new File(var).getCanonicalFile();
		if (!base.isDirectory()) { throw new FileNotFoundException(String.format("Could not find the directory [%s]",
			base.getAbsolutePath())); }

		// Next, does dctm.jar exist in there?
		tgt = new File(base, CMSMFLauncher.DCTM_JAR);
		if (!tgt.isFile()) { throw new FileNotFoundException(String.format("Could not find the JAR file [%s]",
			tgt.getAbsolutePath())); }

		// Next, to the classpath
		ClasspathPatcher.addToClassPath(tgt);
	}

	static Properties getParameterProperties() {
		return CMSMFLauncher.PARAMETER_PROPERTIES;
	}

	public static void main(String[] args) throws Throwable {
		System.setProperty("logName", "cmsmf-startup");
		Map<CLIParam, String> cliParams = AbstractLauncher.parseArguments(args);
		if (cliParams == null) { return; }

		// Just make sure it's initialized
		CMSMFLauncher.patchClasspath(cliParams);

		// Configure Log4J
		final String mode = cliParams.get(CLIParam.mode);
		String log4j = cliParams.get(CLIParam.log4j);
		boolean customLog4j = false;
		if (log4j != null) {
			final File cfg = new File(log4j);
			if (cfg.exists() && cfg.isFile() && cfg.canRead()) {
				LogManager.resetConfiguration();
				DOMConfigurator.configure(cfg.toURI().toURL());
				customLog4j = true;
			}
		}
		if (!customLog4j) {
			LogManager.resetConfiguration();
			String logName = cliParams.get(CLIParam.log_name);
			if (logName == null) {
				String runTime = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
				logName = String.format("cmsmf-%s-%s", mode.toLowerCase(), runTime);
			}
			System.setProperty("logName", logName);
		}

		// Now, convert the command-line parameters into configuration properties
		for (CLIParam p : CLIParam.values()) {
			String value = CMSMFLauncher.getParameter(p);
			if ((value != null) && (p.property != null)) {
				final String key = p.property.name;
				if ((key != null) && (value != null)) {
					CMSMFLauncher.PARAMETER_PROPERTIES.setProperty(key, value);
				}
			}
		}

		// Finally, launch the main class
		// We launch like this because we have to patch the classpath before we link into the rest
		// of the code. If we don't do it like this, the app will refuse to launch altogether
		Class<?> klass = Class.forName(String.format(CMSMFLauncher.MAIN_CLASS, mode));
		CMSMFMain main = null;
		if (CMSMFMain.class.isAssignableFrom(klass)) {
			main = CMSMFMain.class.cast(klass.newInstance());
		} else {
			throw new RuntimeException(String.format("Class [%s] is not a valid AbstractCMSMFMain class",
				klass.getName()));
		}

		// Lock for single execution
		try {
			main.run();
		} finally {
			// Unlock from single execution
		}
	}
}