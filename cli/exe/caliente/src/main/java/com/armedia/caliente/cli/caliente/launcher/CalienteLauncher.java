package com.armedia.caliente.cli.caliente.launcher;

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
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.xml.DOMConfigurator;

import com.armedia.caliente.cli.caliente.cfg.CLIParam;
import com.armedia.caliente.cli.caliente.utils.ClasspathPatcher;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.PluggableServiceLocator;
import com.armedia.commons.utilities.PluggableServiceSelector;
import com.armedia.commons.utilities.Tools;

public class CalienteLauncher extends AbstractLauncher {

	static final Pattern ENGINE_PARSER = Pattern.compile("^\\w+$");
	private static final String MAIN_CLASS = "com.armedia.caliente.cli.caliente.launcher.%s.Caliente_%s";
	private static Properties PARAMETER_PROPERTIES = new Properties();

	public static final String DEFAULT_LOG_FORMAT = "caliente-${logEngine}-${logMode}-${logTimeStamp}";

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
					str = IOUtils.toString(in, "UTF-8");
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
		return CalienteLauncher.PARAMETER_PROPERTIES;
	}

	public static void main(String[] args) throws Throwable {
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
		Matcher m = CalienteLauncher.ENGINE_PARSER.matcher(engine);
		if (!m.matches()) { throw new IllegalArgumentException(
			String.format("Invalid --engine parameter value [%s] - must only contain [a-zA-Z_0-9]", engine)); }

		String logMode = mode.toLowerCase();
		String logEngine = engine.toLowerCase();
		String logTimeStamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
		String logName = CLIParam.log.getString();
		if (logName == null) {
			logName = CalienteLauncher.DEFAULT_LOG_FORMAT;
		}
		System.setProperty("logName", logName);
		System.setProperty("logTimeStamp", logTimeStamp);
		System.setProperty("logMode", logMode);
		System.setProperty("logEngine", logEngine);

		String logCfg = CLIParam.log_cfg.getString();
		boolean customLog = false;
		if (logCfg != null) {
			final File cfg = new File(logCfg);
			if (cfg.exists() && cfg.isFile() && cfg.canRead()) {
				DOMConfigurator.configureAndWatch(cfg.getCanonicalPath());
				customLog = true;
			}
		}
		if (!customLog) {
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
		console.info(String.format("Launching Caliente v%s %s mode for engine %s%n", CalienteLauncher.VERSION,
			CLIParam.mode.getString(), engine));
		Runtime runtime = Runtime.getRuntime();
		console.info(String.format("Current heap size: %d MB", runtime.totalMemory() / 1024 / 1024));
		console.info(String.format("Maximum heap size: %d MB", runtime.maxMemory() / 1024 / 1024));

		Set<URL> patches = new LinkedHashSet<>();
		PluggableServiceSelector<ClasspathPatcher> selector = new PluggableServiceSelector<ClasspathPatcher>() {
			@Override
			public boolean matches(ClasspathPatcher p) {
				return p.supportsEngine(engine);
			}
		};
		PluggableServiceLocator<ClasspathPatcher> patchers = new PluggableServiceLocator<>(ClasspathPatcher.class,
			selector);
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
			List<String> values = p.getAllString();
			if ((values != null) && !values.isEmpty() && (p.property != null)) {
				final String key = p.property.name;
				if ((key != null) && !values.isEmpty()) {
					CalienteLauncher.PARAMETER_PROPERTIES.setProperty(key, StringUtils.join(values, ','));
				}
			}
		}

		// Finally, launch the main class
		// We launch like this because we have to patch the classpath before we link into the rest
		// of the code. If we don't do it like this, the app will refuse to launch altogether
		final Class<?> klass;
		try {
			klass = Class.forName(String.format(CalienteLauncher.MAIN_CLASS, engine, mode));
		} catch (ClassNotFoundException e) {
			System.err.printf("ERROR: Failed to locate a class to support [%s] mode from the [%s] engine", mode,
				engine);
			return;
		}

		final CalienteMain main;
		if (CalienteMain.class.isAssignableFrom(klass)) {
			main = CalienteMain.class.cast(klass.newInstance());
		} else {
			throw new RuntimeException(String.format("Class [%s] is not a valid CalienteMain class", klass.getName()));
		}

		// Lock for single execution
		CmfObjectStore<?, ?> store = main.getObjectStore();
		final boolean writeProperties = (store != null);
		final String pfx = String.format("caliente.%s.%s", engine, mode);
		try {
			if (writeProperties) {
				store.setProperty(String.format("%s.version", pfx), new CmfValue(CalienteLauncher.VERSION));
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