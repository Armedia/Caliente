package com.armedia.caliente.cli.caliente.newlauncher;

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
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.PluggableServiceLocator;
import com.armedia.commons.utilities.Tools;

public abstract class EngineProxy implements AutoCloseable {

	private static final ReadWriteLock PROXIES_LOCK = new ReentrantReadWriteLock();
	private static final AtomicBoolean PROXIES_INITIALIZED = new AtomicBoolean(false);
	private static Map<String, String> PROXY_ALIASES = null;
	private static Map<String, EngineProxy> PROXIES = null;

	private static String canonicalizeName(String name) {
		Objects.requireNonNull(name, "Name may not be null");
		name = StringUtils.strip(name);
		name = StringUtils.lowerCase(name);
		return name;
	}

	private static void initializeProxies(final Logger log) {
		final Lock read = EngineProxy.PROXIES_LOCK.readLock();
		final Lock write = EngineProxy.PROXIES_LOCK.writeLock();

		read.lock();
		try {
			if (!EngineProxy.PROXIES_INITIALIZED.get()) {
				read.unlock();
				write.lock();
				try {
					if (!EngineProxy.PROXIES_INITIALIZED.get()) {
						final PluggableServiceLocator<EngineProxy> proxyInstances = new PluggableServiceLocator<>(
							EngineProxy.class);
						proxyInstances.setHideErrors(log == null);
						if (!proxyInstances.isHideErrors()) {
							proxyInstances.setErrorListener(new PluggableServiceLocator.ErrorListener() {
								@Override
								public void errorRaised(Class<?> serviceClass, Throwable t) {
									log.error("Failed to initialize the EngineProxy class {}",
										serviceClass.getCanonicalName(), t);
								}
							});
						}
						Map<String, String> proxyAliases = new TreeMap<>();
						Map<String, EngineProxy> proxies = new TreeMap<>();
						for (EngineProxy proxy : proxyInstances) {
							final String canonicalName = EngineProxy.canonicalizeName(proxy.getName());
							if (proxies.containsKey(canonicalName)) {
								EngineProxy oldProxy = proxies.get(canonicalName);
								String msg = String.format(
									"EngineProxy title conflict on canonical title [%s] between classes%n\t[%s]=[%s]%n\t[%s]=[%s]",
									canonicalName, oldProxy.getClass().getCanonicalName(), oldProxy.getName(),
									proxy.getClass().getCanonicalName(), proxy.getName());
								if (log != null) {
									log.warn(msg);
								} else {
									throw new IllegalStateException(msg);
								}
							}
							// Add the proxy
							proxies.put(canonicalName, proxy);
							// Add the identity alias mapping
							proxyAliases.put(canonicalName, canonicalName);

							for (String alias : proxy.getAliases()) {
								alias = EngineProxy.canonicalizeName(alias);
								if (StringUtils.equals(canonicalName, alias)) {
									continue;
								}

								if (proxyAliases.containsKey(alias)) {
									EngineProxy oldProxy = proxies.get(proxyAliases.get(alias));
									String msg = String.format(
										"EngineProxy alias conflict on alias [%s] between classes%n\t[%s]=[%s]%n\t[%s]=[%s]",
										alias, oldProxy.getClass().getCanonicalName(), oldProxy.getName(),
										proxy.getClass().getCanonicalName(), proxy.getName());
									if (log != null) {
										log.warn(msg);
									} else {
										throw new IllegalStateException(msg);
									}
								}
								// Add the alias mapping
								proxyAliases.put(alias, canonicalName);
							}
							EngineProxy.PROXIES = Tools.freezeMap(new LinkedHashMap<>(proxies));
							EngineProxy.PROXY_ALIASES = Tools.freezeMap(new LinkedHashMap<>(proxyAliases));
						}

						EngineProxy.PROXIES_INITIALIZED.set(true);
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

	public static EngineProxy get(final Logger log, final String engine) {
		if (StringUtils
			.isEmpty(engine)) { throw new IllegalArgumentException("Must provide a non-empty engine title"); }

		EngineProxy.initializeProxies(log);

		String canonicalEngine = EngineProxy.PROXY_ALIASES.get(EngineProxy.canonicalizeName(engine));
		if (canonicalEngine == null) {
			// No such engine!
			return null;
		}

		return EngineProxy.PROXIES.get(canonicalEngine);
	}

	public static EngineProxy get(final String engine) {
		return EngineProxy.get(null, engine);
	}

	public static Collection<String> getAliases(final Logger log) {
		EngineProxy.initializeProxies(log);
		return EngineProxy.PROXY_ALIASES.keySet();
	}

	protected final Logger log = LoggerFactory.getLogger(getClass());

	public abstract String getName();

	public abstract Set<String> getAliases();

	public abstract CmfCrypt getCrypt();

	public final CommandModule<?> getCommandModule(CalienteCommand command) {
		Objects.requireNonNull(command, "Must provide a non-null command");
		switch (command) {
			case COUNT:
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
		switch (command) {
			case ENCRYPT:
				return new EncryptCommandModule(transferEngine);
			case DECRYPT:
				return new DecryptCommandModule(transferEngine);
			default:
				break;
		}

		throw new IllegalArgumentException(
			String.format("The command [%s] is unsupported at this time", command.title));
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

	@Override
	public void close() throws Exception {
		// By default, do nothing...
	}
}