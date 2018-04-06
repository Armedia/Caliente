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
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.tools.CmfCrypt;
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

	public static final CmfCrypt CRYPTO = new CmfCrypt();

	int run( //
		@SuppressWarnings("rawtypes") final EngineFactory engineFactory, //
		@SuppressWarnings("rawtypes") final CmfObjectStore objectStore, //
		@SuppressWarnings("rawtypes") final CmfContentStore contentStore, //
		final CommandModule command, //
		final OptionValues commandValues, //
		final Collection<String> positionals //
	) throws Exception {

		// Now, convert the command-line parameters into configuration properties
		for (OptionValue v : commandValues) {

			List<String> values = v.getAllStrings();
			if ((values != null) && !values.isEmpty()) {
				// Store this parameter value as a property
			}
		}

		// Lock for single execution
		final boolean writeProperties = (objectStore != null);
		final String pfx = String.format("caliente.%s.%s", engineFactory.getName().toLowerCase(),
			command.getDescriptor().getName().toLowerCase());
		try {
			if (writeProperties) {
				objectStore.setProperty(String.format("%s.version", pfx), new CmfValue(Caliente.VERSION));
				objectStore.setProperty(String.format("%s.start", pfx), new CmfValue(new Date()));
			}
			command.run(engineFactory, objectStore, contentStore, commandValues, positionals);
		} catch (Throwable t) {
			if (writeProperties) {
				objectStore.setProperty(String.format("%s.error", pfx), new CmfValue(Tools.dumpStackTrace(t)));
			}
			throw new RuntimeException("Execution failed", t);
		} finally {
			// Unlock from single execution
			if (writeProperties) {
				objectStore.setProperty(String.format("%s.end", pfx), new CmfValue(new Date()));
			}
		}
		return 0;
	}
}