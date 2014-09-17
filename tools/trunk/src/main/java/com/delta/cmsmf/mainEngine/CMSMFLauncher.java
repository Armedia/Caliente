package com.delta.cmsmf.mainEngine;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.apache.log4j.LogManager;
import org.apache.log4j.xml.DOMConfigurator;

import com.delta.cmsmf.cfg.CLIParam;

public class CMSMFLauncher extends AbstractLauncher {

	private static final String MAIN_CLASS = "com.delta.cmsmf.mainEngine.CMSMFMain_%s";

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

		// Just make sure it's initialized
		AbstractLauncher.patchClasspath();

		// Configure Log4J
		final String mode = CLIParam.mode.getString();
		String log4j = CLIParam.log4j.getString();
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
			String logName = CLIParam.log_name.getString();
			if (logName == null) {
				String runTime = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
				logName = String.format("cmsmf-%s-%s", mode.toLowerCase(), runTime);
			}
			System.setProperty("logName", logName);
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