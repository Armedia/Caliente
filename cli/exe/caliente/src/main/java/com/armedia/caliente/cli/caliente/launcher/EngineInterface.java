package com.armedia.caliente.cli.caliente.launcher;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.caliente.command.CalienteCommand;
import com.armedia.caliente.cli.caliente.command.CommandModule;
import com.armedia.caliente.cli.caliente.command.DecryptCommandModule;
import com.armedia.caliente.cli.caliente.command.EncryptCommandModule;
import com.armedia.caliente.cli.caliente.command.ExportCommandModule;
import com.armedia.caliente.cli.caliente.command.ImportCommandModule;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.engine.TransferEngine;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.importer.ImportEngine;
import com.armedia.commons.utilities.PluggableServiceLocator;
import com.armedia.commons.utilities.Tools;

public abstract class EngineInterface {

	private static final ReadWriteLock INTERFACES_LOCK = new ReentrantReadWriteLock();
	private static final AtomicBoolean INTERFACES_INITIALIZED = new AtomicBoolean(false);
	private static Map<String, String> ENGINE_ALIASES = null;
	private static Map<String, EngineInterface> INTERFACES = null;

	private static String canonicalizeName(String name) {
		Objects.requireNonNull(name, "Name may not be null");
		name = StringUtils.strip(name);
		name = StringUtils.lowerCase(name);
		return name;
	}

	private static void initializeInterfaces(final Logger log) {
		final Lock read = EngineInterface.INTERFACES_LOCK.readLock();
		final Lock write = EngineInterface.INTERFACES_LOCK.writeLock();

		read.lock();
		try {
			if (!EngineInterface.INTERFACES_INITIALIZED.get()) {
				read.unlock();
				write.lock();
				try {
					if (!EngineInterface.INTERFACES_INITIALIZED.get()) {
						final PluggableServiceLocator<EngineInterface> engineInterfaces = new PluggableServiceLocator<>(
							EngineInterface.class);
						engineInterfaces.setHideErrors(log == null);
						if (!engineInterfaces.isHideErrors()) {
							engineInterfaces.setErrorListener(new PluggableServiceLocator.ErrorListener() {
								@Override
								public void errorRaised(Class<?> serviceClass, Throwable t) {
									log.error("Failed to initialize the EngineInterface class {}",
										serviceClass.getCanonicalName(), t);
								}
							});
						}
						Map<String, String> engineAliases = new TreeMap<>();
						Map<String, EngineInterface> interfaces = new TreeMap<>();
						for (EngineInterface engineInterface : engineInterfaces) {
							final String canonicalName = EngineInterface.canonicalizeName(engineInterface.getName());
							if (interfaces.containsKey(canonicalName)) {
								EngineInterface oldInterface = interfaces.get(canonicalName);
								String msg = String.format(
									"EngineInterface title conflict on canonical title [%s] between classes%n\t[%s]=[%s]%n\t[%s]=[%s]",
									canonicalName, oldInterface.getClass().getCanonicalName(), oldInterface.getName(),
									engineInterface.getClass().getCanonicalName(), engineInterface.getName());
								if (log != null) {
									log.warn(msg);
								} else {
									throw new IllegalStateException(msg);
								}
							}
							// Add the proxy
							interfaces.put(canonicalName, engineInterface);
							// Add the identity alias mapping
							engineAliases.put(canonicalName, canonicalName);

							for (String alias : engineInterface.getAliases()) {
								alias = EngineInterface.canonicalizeName(alias);
								if (StringUtils.equals(canonicalName, alias)) {
									continue;
								}

								if (engineAliases.containsKey(alias)) {
									EngineInterface oldInterface = interfaces.get(engineAliases.get(alias));
									String msg = String.format(
										"EngineInterface alias conflict on alias [%s] between classes%n\t[%s]=[%s]%n\t[%s]=[%s]",
										alias, oldInterface.getClass().getCanonicalName(), oldInterface.getName(),
										engineInterface.getClass().getCanonicalName(), engineInterface.getName());
									if (log != null) {
										log.warn(msg);
									} else {
										throw new IllegalStateException(msg);
									}
								}
								// Add the alias mapping
								engineAliases.put(alias, canonicalName);
							}
							EngineInterface.INTERFACES = Tools.freezeMap(new LinkedHashMap<>(interfaces));
							EngineInterface.ENGINE_ALIASES = Tools.freezeMap(new LinkedHashMap<>(engineAliases));
						}

						EngineInterface.INTERFACES_INITIALIZED.set(true);
					}
					read.lock();
				} finally {
					write.unlock();
				}
			}
		} finally {
			read.unlock();
		}

	}

	public static EngineInterface get(final Logger log, final String engine) {
		if (StringUtils
			.isEmpty(engine)) { throw new IllegalArgumentException("Must provide a non-empty engine title"); }

		EngineInterface.initializeInterfaces(log);

		String canonicalEngine = EngineInterface.ENGINE_ALIASES.get(EngineInterface.canonicalizeName(engine));
		if (canonicalEngine == null) {
			// No such engine!
			return null;
		}

		return EngineInterface.INTERFACES.get(canonicalEngine);
	}

	public static EngineInterface get(final String engine) {
		return EngineInterface.get(null, engine);
	}

	public static Collection<String> getAliases(final Logger log) {
		EngineInterface.initializeInterfaces(log);
		return EngineInterface.ENGINE_ALIASES.keySet();
	}

	protected final Logger log = LoggerFactory.getLogger(getClass());

	public abstract String getName();

	public abstract Set<String> getAliases();

	public final CommandModule<?> getCommandModule(CalienteCommand command) {
		Objects.requireNonNull(command, "Must provide a non-null command");
		switch (command) {
			case COUNT:
				// TODO: add a counter interface
				break;

			case EXPORT:
				ExportEngine<?, ?, ?, ?, ?, ?> exportEngine = getExportEngine();
				if (exportEngine == null) { throw new IllegalStateException(
					"This proxy does not support an Export engine"); }
				return newExporter(exportEngine);

			case IMPORT:
				ImportEngine<?, ?, ?, ?, ?, ?> importEngine = getImportEngine();
				if (importEngine == null) { throw new IllegalStateException(
					"This proxy does not support an Import engine"); }
				return newImporter(importEngine);

			default:
				break;
		}

		TransferEngine<?, ?, ?, ?, ?, ?> transferEngine = getImportEngine();
		if (transferEngine == null) {
			transferEngine = getExportEngine();
		}
		if (transferEngine == null) { throw new IllegalStateException("This proxy does not support an Import engine"); }
		switch (command) {
			case ENCRYPT:
				return newEncryptor(transferEngine);
			case DECRYPT:
				return newDecryptor(transferEngine);
			default:
				break;
		}

		throw new IllegalArgumentException(
			String.format("The command [%s] is unsupported at this time", command.title));
	}

	protected EncryptCommandModule newEncryptor(TransferEngine<?, ?, ?, ?, ?, ?> engine) {
		return new EncryptCommandModule(engine);
	}

	protected DecryptCommandModule newDecryptor(TransferEngine<?, ?, ?, ?, ?, ?> engine) {
		return new DecryptCommandModule(engine);
	}

	protected ExportCommandModule newExporter(ExportEngine<?, ?, ?, ?, ?, ?> engine) {
		return new ExportCommandModule(engine);
	}

	protected abstract ExportEngine<?, ?, ?, ?, ?, ?> getExportEngine();

	protected ImportCommandModule newImporter(ImportEngine<?, ?, ?, ?, ?, ?> engine) {
		return new ImportCommandModule(engine);
	}

	protected abstract ImportEngine<?, ?, ?, ?, ?, ?> getImportEngine();

	public abstract Collection<? extends LaunchClasspathHelper> getClasspathHelpers();
}