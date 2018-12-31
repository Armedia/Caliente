package com.armedia.caliente.cli.caliente.launcher;

import java.util.Collection;
import java.util.Collections;
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

import com.armedia.caliente.cli.caliente.command.CalienteCommand;
import com.armedia.caliente.cli.caliente.command.CommandModule;
import com.armedia.caliente.cli.caliente.command.DecryptCommandModule;
import com.armedia.caliente.cli.caliente.command.EncryptCommandModule;
import com.armedia.caliente.cli.caliente.command.ExportCommandModule;
import com.armedia.caliente.cli.caliente.command.ImportCommandModule;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.engine.TransferEngineFactory;
import com.armedia.caliente.engine.exporter.ExportEngineFactory;
import com.armedia.caliente.engine.importer.ImportEngineFactory;
import com.armedia.commons.utilities.PluggableServiceLocator;
import com.armedia.commons.utilities.Tools;

public abstract class AbstractEngineInterface {

	private static final ReadWriteLock INTERFACES_LOCK = new ReentrantReadWriteLock();
	private static final AtomicBoolean INTERFACES_INITIALIZED = new AtomicBoolean(false);
	private static Map<String, String> ENGINE_ALIASES = null;
	private static Map<String, AbstractEngineInterface> INTERFACES = null;

	private static String canonicalizeName(String name) {
		Objects.requireNonNull(name, "Name may not be null");
		name = StringUtils.strip(name);
		name = StringUtils.lowerCase(name);
		return name;
	}

	private static void initializeInterfaces(final Logger log) {
		final Lock read = AbstractEngineInterface.INTERFACES_LOCK.readLock();
		final Lock write = AbstractEngineInterface.INTERFACES_LOCK.writeLock();

		read.lock();
		try {
			if (!AbstractEngineInterface.INTERFACES_INITIALIZED.get()) {
				read.unlock();
				write.lock();
				try {
					if (!AbstractEngineInterface.INTERFACES_INITIALIZED.get()) {
						final PluggableServiceLocator<AbstractEngineInterface> abstractEngineInterfaces = new PluggableServiceLocator<>(
							AbstractEngineInterface.class);
						abstractEngineInterfaces.setHideErrors(log == null);
						if (!abstractEngineInterfaces.isHideErrors()) {
							abstractEngineInterfaces.setErrorListener(new PluggableServiceLocator.ErrorListener() {
								@Override
								public void errorRaised(Class<?> serviceClass, Throwable t) {
									log.error("Failed to initialize the EngineInterface class {}",
										serviceClass.getCanonicalName(), t);
								}
							});
						}
						Map<String, String> engineAliases = new TreeMap<>();
						Map<String, AbstractEngineInterface> interfaces = new TreeMap<>();
						for (AbstractEngineInterface abstractEngineInterface : abstractEngineInterfaces) {
							final String canonicalName = AbstractEngineInterface
								.canonicalizeName(abstractEngineInterface.getName());
							if (interfaces.containsKey(canonicalName)) {
								AbstractEngineInterface oldInterface = interfaces.get(canonicalName);
								String msg = String.format(
									"EngineInterface title conflict on canonical title [%s] between classes%n\t[%s]=[%s]%n\t[%s]=[%s]",
									canonicalName, oldInterface.getClass().getCanonicalName(), oldInterface.getName(),
									abstractEngineInterface.getClass().getCanonicalName(),
									abstractEngineInterface.getName());
								if (log != null) {
									log.warn(msg);
								} else {
									throw new IllegalStateException(msg);
								}
							}
							// Add the proxy
							interfaces.put(canonicalName, abstractEngineInterface);
							// Add the identity alias mapping
							engineAliases.put(canonicalName, canonicalName);

							for (String alias : abstractEngineInterface.getAliases()) {
								alias = AbstractEngineInterface.canonicalizeName(alias);
								if (StringUtils.equals(canonicalName, alias)) {
									continue;
								}

								if (engineAliases.containsKey(alias)) {
									AbstractEngineInterface oldInterface = interfaces.get(engineAliases.get(alias));
									String msg = String.format(
										"EngineInterface alias conflict on alias [%s] between classes%n\t[%s]=[%s]%n\t[%s]=[%s]",
										alias, oldInterface.getClass().getCanonicalName(), oldInterface.getName(),
										abstractEngineInterface.getClass().getCanonicalName(),
										abstractEngineInterface.getName());
									if (log != null) {
										log.warn(msg);
									} else {
										throw new IllegalStateException(msg);
									}
								}
								// Add the alias mapping
								engineAliases.put(alias, canonicalName);
							}
						}

						if (interfaces.isEmpty()) {
							AbstractEngineInterface.INTERFACES = Collections.emptyMap();
							AbstractEngineInterface.ENGINE_ALIASES = Collections.emptyMap();
							log.warn(
								"No engine interfaces were located. Please check the contents of the service file for {}",
								AbstractEngineInterface.class.getCanonicalName());
						} else {
							AbstractEngineInterface.INTERFACES = Tools.freezeMap(new LinkedHashMap<>(interfaces));
							AbstractEngineInterface.ENGINE_ALIASES = Tools
								.freezeMap(new LinkedHashMap<>(engineAliases));
						}
						AbstractEngineInterface.INTERFACES_INITIALIZED.set(true);
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

	public static AbstractEngineInterface get(final Logger log, final String engine) {
		if (StringUtils
			.isEmpty(engine)) { throw new IllegalArgumentException("Must provide a non-empty engine title"); }

		AbstractEngineInterface.initializeInterfaces(log);

		String canonicalEngine = AbstractEngineInterface.ENGINE_ALIASES
			.get(AbstractEngineInterface.canonicalizeName(engine));
		if (canonicalEngine == null) {
			// No such engine!
			return null;
		}

		return AbstractEngineInterface.INTERFACES.get(canonicalEngine);
	}

	public static AbstractEngineInterface get(final String engine) {
		return AbstractEngineInterface.get(null, engine);
	}

	public static Collection<String> getAliases(final Logger log) {
		AbstractEngineInterface.initializeInterfaces(log);
		return AbstractEngineInterface.ENGINE_ALIASES.keySet();
	}

	// protected final Logger log = LoggerFactory.getLogger(getClass());

	public abstract String getName();

	public abstract Set<String> getAliases();

	final CommandModule<?> getCommandModule(CalienteCommand command) {
		Objects.requireNonNull(command, "Must provide a non-null command");
		switch (command) {
			case COUNT:
				// TODO: add a counter interface
				break;

			case EXPORT:
				ExportEngineFactory<?, ?, ?, ?, ?, ?> exportEngine = getExportEngine();
				if (exportEngine == null) { return null; }
				return newExporter(exportEngine);

			case IMPORT:
				ImportEngineFactory<?, ?, ?, ?, ?, ?> importEngine = getImportEngine();
				if (importEngine == null) { return null; }
				return newImporter(importEngine);

			default:
				break;
		}

		TransferEngineFactory<?, ?, ?, ?, ?, ?, ?, ?, ?> transferEngineFactory = getImportEngine();
		if (transferEngineFactory == null) {
			transferEngineFactory = getExportEngine();
		}
		if (transferEngineFactory != null) {
			switch (command) {
				case ENCRYPT:
					return newEncryptor(transferEngineFactory);
				case DECRYPT:
					return newDecryptor(transferEngineFactory);
				default:
					break;
			}
		}

		return null;
	}

	protected EncryptCommandModule newEncryptor(TransferEngineFactory<?, ?, ?, ?, ?, ?, ?, ?, ?> engine) {
		return new EncryptCommandModule(engine);
	}

	protected DecryptCommandModule newDecryptor(TransferEngineFactory<?, ?, ?, ?, ?, ?, ?, ?, ?> engine) {
		return new DecryptCommandModule(engine);
	}

	protected ExportCommandModule newExporter(ExportEngineFactory<?, ?, ?, ?, ?, ?> engine) {
		return new ExportCommandModule(engine);
	}

	protected abstract ExportEngineFactory<?, ?, ?, ?, ?, ?> getExportEngine();

	protected ImportCommandModule newImporter(ImportEngineFactory<?, ?, ?, ?, ?, ?> engine) {
		return new ImportCommandModule(engine);
	}

	protected abstract ImportEngineFactory<?, ?, ?, ?, ?, ?> getImportEngine();

	public abstract Collection<? extends LaunchClasspathHelper> getClasspathHelpers();
}