package com.armedia.caliente.cli.caliente.newlauncher;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import com.armedia.caliente.cli.OptionValue;
import com.armedia.caliente.cli.OptionValues;
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

	int run(@SuppressWarnings("rawtypes") EngineFactory engineFactory, CommandModule command,
		OptionValues commandValues, Collection<String> positionals) throws Exception {

		// Now, convert the command-line parameters into configuration properties
		for (OptionValue v : commandValues) {

			List<String> values = v.getAllStrings();
			if ((values != null) && !values.isEmpty()) {
				// Store this parameter value as a property
			}
		}

		final CmfObjectStore<?, ?> store = command.getObjectStore();
		// Lock for single execution
		final boolean writeProperties = (store != null);
		final String pfx = String.format("caliente.%s.%s", engineFactory.getName().toLowerCase(),
			command.getName().toLowerCase());
		try {
			if (writeProperties) {
				store.setProperty(String.format("%s.version", pfx), new CmfValue(Caliente.VERSION));
				store.setProperty(String.format("%s.start", pfx), new CmfValue(new Date()));
			}
			command.run();
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