package com.delta.cmsmf.mainEngine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.xml.DOMConfigurator;

import com.delta.cmsmf.constants.CMSMFAppConstants;
import com.delta.cmsmf.datastore.DataStore;
import com.delta.cmsmf.properties.PropertiesManager;

public class CMSMFLauncher extends AbstractLauncher {

	private static final String ENV_DOCUMENTUM_SHARED = "DOCUMENTUM_SHARED";
	private static final String ENV_DOCUMENTUM = "DOCUMENTUM";
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
			METHOD = URLClassLoader.class.getDeclaredMethod("addURL", CMSMFLauncher.PARAMETERS);
			CMSMFLauncher.METHOD.setAccessible(true);
		} catch (Throwable t) {
			throw new RuntimeException("Failed to initialize access to the addURL() method in the system classloader",
				t);
		}
	}

	private static void addToClassPath(URL u) throws IOException {
		try {
			CMSMFLauncher.METHOD.invoke(CMSMFLauncher.CL, u);
		} catch (Throwable t) {
			throw new IOException(String.format("Failed to add the URL [%s] to the system classloader", u), t);
		}
	}

	private static void addToClassPath(File f) throws IOException {
		CMSMFLauncher.addToClassPath(f.toURI().toURL());
	}

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
		CMSMFLauncher.addToClassPath(base);
		CMSMFLauncher.addToClassPath(tgt);

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

		CMSMFLauncher.addToClassPath(tgt);

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
		CMSMFLauncher.addToClassPath(tgt);
	}

	public static void main(String[] args) throws Throwable {
		Map<CLIParam, String> cliParams = AbstractLauncher.parseArguments(args);
		if (cliParams == null) { return; }

		// Configure Log4J
		String log4j = cliParams.get(CLIParam.log4j);
		if (log4j != null) {
			final File cfg = new File(log4j);
			if (cfg.exists() && cfg.isFile() && cfg.canRead()) {
				DOMConfigurator.configure(cfg.toURI().toURL());
			}
		}

		CMSMFLauncher.patchClasspath(cliParams);

		// Now, convert the command-line parameters into configuration properties
		Properties parameters = new Properties();
		for (CLIParam p : CLIParam.values()) {
			String value = CMSMFLauncher.getParameter(p);
			if ((value != null) && (p.property != null)) {
				final String key = p.property.name;
				if ((key != null) && (value != null)) {
					parameters.setProperty(key, value);
				}
			}
		}

		// If we have command-line parameters, these supersede all other configurations, even if
		// we have a configuration file explicitly listed.
		if (!parameters.isEmpty()) {
			PropertiesManager.addPropertySource(parameters);
		}

		// A configuration file has been specifed, so use its values ahead of the defaults
		if (CMSMFLauncher.getParameter(CLIParam.cfg) != null) {
			PropertiesManager.addPropertySource(CMSMFLauncher.getParameter(CLIParam.cfg));
		}

		// Now, the catch-all, default configuration
		PropertiesManager.addPropertySource(CMSMFAppConstants.FULLY_QUALIFIED_CONFIG_FILE_NAME);

		// And we start up the configuration engine...
		PropertiesManager.init();

		// Finally, launch the main class
		// We launch like this because we have to patch the classpath before we link into the rest
		// of the code. If we don't do it like this, the app will refuse to launch altogether
		String mode = cliParams.get(CLIParam.mode);
		Class<?> klass = Class.forName(String.format(CMSMFLauncher.MAIN_CLASS, mode));
		CMSMFMain main = null;
		if (CMSMFMain.class.isAssignableFrom(klass)) {
			main = CMSMFMain.class.cast(klass.newInstance());
		} else {
			throw new RuntimeException(String.format("Class [%s] is not a valid AbstractCMSMFMain class",
				klass.getName()));
		}

		if (main.requiresDataStore()) {
			DataStore.init(main.requiresCleanData());
		}
		main.run();
	}
}