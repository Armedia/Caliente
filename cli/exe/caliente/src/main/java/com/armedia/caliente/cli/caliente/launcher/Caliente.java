package com.armedia.caliente.cli.caliente.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.caliente.cfg.CalienteState;
import com.armedia.caliente.cli.caliente.command.CommandModule;
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
		final String engineName, //
		final CalienteState state, final CommandModule<?> command, //
		final OptionValues commandValues, //
		final Collection<String> positionals //
	) throws Exception {
		// TODO: Lock for single execution
		final Logger log = LoggerFactory.getLogger(getClass());
		final CmfObjectStore<?, ?> objectStore = state.getObjectStore();
		final boolean writeProperties = (objectStore != null);
		final String pfx = String.format("caliente.%s.%s", engineName.toLowerCase(),
			command.getDescriptor().getTitle().toLowerCase());
		try {
			if (writeProperties) {
				Map<String, CmfValue> properties = new TreeMap<>();
				properties.put(String.format("%s.version", pfx), new CmfValue(Caliente.VERSION));
				properties.put(String.format("%s.start", pfx), new CmfValue(new Date()));
				objectStore.setProperties(properties);
			}
			command.run(state, commandValues, positionals);
		} catch (Throwable t) {
			if (writeProperties) {
				try {
					objectStore.setProperty(String.format("%s.error", pfx), new CmfValue(Tools.dumpStackTrace(t)));
				} catch (Exception e) {
					log.error("Failed to store the captured error into the properties database", e);
				}
			}
			throw new RuntimeException("Execution failed", t);
		} finally {
			// TODO: Unlock from single execution
			if (writeProperties) {
				objectStore.setProperty(String.format("%s.end", pfx), new CmfValue(new Date()));
			}
		}
		return 0;
	}
}