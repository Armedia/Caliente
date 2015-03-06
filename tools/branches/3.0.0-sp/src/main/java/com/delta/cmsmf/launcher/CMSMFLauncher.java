package com.delta.cmsmf.launcher;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.xml.DOMConfigurator;

import com.armedia.commons.utilities.PluggableServiceLocator;
import com.armedia.commons.utilities.PluggableServiceSelector;
import com.delta.cmsmf.cfg.CLIParam;
import com.delta.cmsmf.utils.ClasspathPatcher;

public class CMSMFLauncher extends AbstractLauncher {

	static final Pattern SERVER_PARSER = Pattern.compile("^([^:]+):(.+)$");
	private static final String MAIN_CLASS = "com.delta.cmsmf.launcher.CMSMFMain_%s_%s";

	private static Properties PARAMETER_PROPERTIES = new Properties();

	static Properties getParameterProperties() {
		return CMSMFLauncher.PARAMETER_PROPERTIES;
	}

	public static void main(String[] args) throws Throwable {
		System.setProperty("logName", "cmsmf-startup");
		if (!CLIParam.parse(args)) {
			// If the parameters didn't parse, we fail.
			return;
		}

		// Configure Log4J
		final String mode = CLIParam.mode.getString();
		final String server = CLIParam.server.getString();
		if (server == null) { throw new IllegalArgumentException(String.format("Must provide a --server parameter")); }
		// The server spec will be of the form <engine>:<server-spec>, so parse it as such
		Matcher m = CMSMFLauncher.SERVER_PARSER.matcher(server);
		if (!m.matches()) { throw new IllegalArgumentException(String.format(
			"Invalid --server parameter value [%s] - must match <engine>:<server-spec>", server)); }
		final String engine = m.group(1);

		String log4j = CLIParam.log4j.getString();
		boolean customLog4j = false;
		if (log4j != null) {
			final File cfg = new File(log4j);
			if (cfg.exists() && cfg.isFile() && cfg.canRead()) {
				DOMConfigurator.configureAndWatch(cfg.getCanonicalPath());
				customLog4j = true;
			}
		}
		if (!customLog4j) {
			String logName = CLIParam.log_name.getString();
			if (logName == null) {
				String runTime = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
				logName = String.format("cmsmf-%s-%s-%s", engine.toLowerCase(), mode.toLowerCase(), runTime);
			}
			System.setProperty("logName", logName);
			URL config = Thread.currentThread().getContextClassLoader().getResource("log4j.xml");
			if (config != null) {
				DOMConfigurator.configure(config);
			} else {
				config = Thread.currentThread().getContextClassLoader().getResource("log4j.properties");
				if (config != null) {
					PropertyConfigurator.configure(config);
				}
			}
		}

		// Make sure log4j is configured
		Logger.getRootLogger().info("Logging active");

		List<URL> patches = new ArrayList<URL>();
		PluggableServiceSelector<ClasspathPatcher> selector = new PluggableServiceSelector<ClasspathPatcher>() {
			@Override
			public boolean matches(ClasspathPatcher p) {
				return p.supportsEngine(engine);
			}
		};
		PluggableServiceLocator<ClasspathPatcher> patchers = new PluggableServiceLocator<ClasspathPatcher>(
			ClasspathPatcher.class, selector);
		patchers.setHideErrors(false);
		for (ClasspathPatcher p : patchers) {
			List<URL> l = p.getPatches(engine);
			if ((l == null) || l.isEmpty()) {
				continue;
			}
			for (URL u : l) {
				if (u != null) {
					patches.add(u);
				}
			}
		}

		for (URL u : patches) {
			ClasspathPatcher.addToClassPath(u);
		}

		// Now, convert the command-line parameters into configuration properties
		for (CLIParam p : CLIParam.values()) {
			if (!p.isPresent()) {
				continue;
			}
			String value = p.getString();
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
		final Class<?> klass;
		try {
			klass = Class.forName(String.format(CMSMFLauncher.MAIN_CLASS, mode, engine));
		} catch (ClassNotFoundException e) {
			System.err
			.printf("ERROR: Failed to locate a class to support [%s] mode from the [%s] engine", mode, engine);
			return;
		}

		final CMSMFMain main;
		if (CMSMFMain.class.isAssignableFrom(klass)) {
			main = CMSMFMain.class.cast(klass.newInstance());
		} else {
			throw new RuntimeException(String.format("Class [%s] is not a valid CMSMFMain class", klass.getName()));
		}

		// Lock for single execution
		try {
			main.run();
		} finally {
			// Unlock from single execution
		}
	}
}