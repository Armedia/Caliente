package com.delta.cmsmf.launcher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.xml.DOMConfigurator;

import com.armedia.cmf.storage.CmfObjectStore;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.PluggableServiceLocator;
import com.armedia.commons.utilities.PluggableServiceSelector;
import com.armedia.commons.utilities.Tools;
import com.delta.cmsmf.cfg.CLIParam;
import com.delta.cmsmf.utils.ClasspathPatcher;

public class CMSMFLauncher extends AbstractLauncher {

	static final Pattern ENGINE_PARSER = Pattern.compile("^\\w+$");
	private static final String MAIN_CLASS = "com.delta.cmsmf.launcher.%s.CMSMFMain_%s";

	private static Properties PARAMETER_PROPERTIES = new Properties();

	public static final String VERSION;

	static {
		String version = null;
		URL url = Thread.currentThread().getContextClassLoader().getResource("version.properties");
		if (url != null) {
			Properties p = new Properties();
			try {
				InputStream in = url.openStream();
				final String str;
				try {
					str = IOUtils.toString(in);
				} finally {
					IOUtils.closeQuietly(in);
				}
				p.load(new StringReader(str));
				version = p.getProperty("version");
			} catch (IOException e) {
				version = "(failed to load)";
			}
		}
		VERSION = Tools.coalesce(version, "(unknown)");
	}

	static Properties getParameterProperties() {
		return CMSMFLauncher.PARAMETER_PROPERTIES;
	}

	public static void main(String[] args) throws Throwable {
		System.setProperty("logName", "cmsmf-startup");
		// Temporary, for debugging
		System.setProperty("h2.threadDeadlockDetector", "true");
		if (!CLIParam.parse(args)) {
			// If the parameters didn't parse, we fail.
			return;
		}

		// Configure Log4J
		final String mode = CLIParam.mode.getString();
		final String engine = CLIParam.engine.getString();
		if (engine == null) { throw new IllegalArgumentException(String.format("Must provide a --engine parameter")); }
		Matcher m = CMSMFLauncher.ENGINE_PARSER.matcher(engine);
		if (!m.matches()) { throw new IllegalArgumentException(String.format(
			"Invalid --engine parameter value [%s] - must only contain [a-zA-Z_0-9]", engine)); }

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
		final Logger console = Logger.getLogger("console");
		console.info(String.format("Launching CMSMF v%s %s mode for engine %s%n", CMSMFLauncher.VERSION,
			CLIParam.mode.getString(), engine));
		Runtime runtime = Runtime.getRuntime();
		console.info(String.format("Current heap size: %d MB", runtime.totalMemory() / 1024 / 1024));
		console.info(String.format("Maximum heap size: %d MB", runtime.maxMemory() / 1024 / 1024));

		Set<URL> patches = new LinkedHashSet<URL>();
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
			console.info(String.format("Classpath addition: [%s]", u));
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
			klass = Class.forName(String.format(CMSMFLauncher.MAIN_CLASS, engine, mode));
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
		CmfObjectStore<?, ?> store = main.getObjectStore();
		final boolean writeProperties = (store != null);
		final String pfx = String.format("cmsmf.%s.%s", engine, mode);
		try {
			if (writeProperties) {
				store.setProperty(String.format("%s.version", pfx), new CmfValue(CMSMFLauncher.VERSION));
				store.setProperty(String.format("%s.start", pfx), new CmfValue(new Date()));
			}
			main.run();
		} catch (Throwable t) {
			if (writeProperties) {
				store.setProperty(String.format("%s.error", pfx), new CmfValue(Tools.dumpStackTrace(t)));
			}
			throw new RuntimeException("Execution failed", t);
		} finally {
			// Unlock from single execution
			if (writeProperties) {
				store.setProperty(String.format("%s.end", pfx), new CmfValue(new Date()));
			}
		}
	}
}