package com.armedia.caliente.cli.caliente.newlauncher;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.caliente.cfg.CLIParam;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

public class Caliente {

	/**
	 * Read the Caliente version... is this the cleanest way?
	 */
	public static final String VERSION;
	static {
		String version = null;
		URL url = Thread.currentThread().getContextClassLoader().getResource("version.properties");
		if (url != null) {
			try (InputStream in = url.openStream()) {
				Properties p = new Properties();
				p.load(in);
				version = p.getProperty("version");
			} catch (IOException e) {
				e.printStackTrace(System.err);
				version = "(failed to load)";
			}
		}
		VERSION = Tools.coalesce(version, "(unknown)");
	}

	int run(String command, OptionValues commandValues, Collection<String> positionals) throws Exception {
		// Temporary, for debugging

		// Now, convert the command-line parameters into configuration properties
		for (CLIParam p : CLIParam.values()) {
			if (!p.isPresent()) {
				continue;
			}
			List<String> values = p.getAllString();
			if ((values != null) && !values.isEmpty() && (p.property != null)) {
				final String key = p.property.name;
				if ((key != null) && !values.isEmpty()) {
					// Launcher.PARAMETER_PROPERTIES.setProperty(key, StringUtils.join(values,
					// ','));
				}
			}
		}

		// Finally, launch the main class
		// We launch like this because we have to patch the classpath before we link into the rest
		// of the code. If we don't do it like this, the app will refuse to launch altogether
		final Class<?> klass;
		try {
			klass = Class.forName(String.format(Launcher.MAIN_CLASS, engine, mode));
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
				store.setProperty(String.format("%s.version", pfx), new CmfValue(Launcher.VERSION));
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
		return 0;
	}
}