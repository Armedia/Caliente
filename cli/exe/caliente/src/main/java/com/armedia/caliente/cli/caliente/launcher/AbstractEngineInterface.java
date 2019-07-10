/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.cli.caliente.launcher;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

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
import com.armedia.caliente.engine.TransferEngineFactory;
import com.armedia.caliente.engine.exporter.ExportEngineFactory;
import com.armedia.caliente.engine.importer.ImportEngineFactory;
import com.armedia.commons.utilities.PluggableServiceLocator;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.concurrent.BaseShareableLockable;
import com.armedia.commons.utilities.concurrent.ShareableLockable;

public abstract class AbstractEngineInterface {

	private static final ShareableLockable INTERFACES_LOCK = new BaseShareableLockable();
	private static volatile Map<String, String> ENGINE_ALIASES = null;
	private static volatile Map<String, AbstractEngineInterface> INTERFACES = null;

	private static String canonicalizeName(String name) {
		Objects.requireNonNull(name, "Name may not be null");
		name = StringUtils.strip(name);
		name = StringUtils.lowerCase(name);
		return name;
	}

	private static void initializeInterfaces(final Logger log) {
		AbstractEngineInterface.INTERFACES_LOCK.shareLockedUpgradable(() -> AbstractEngineInterface.INTERFACES,
			Objects::isNull, (m) -> {
				final PluggableServiceLocator<AbstractEngineInterface> abstractEngineInterfaces = new PluggableServiceLocator<>(
					AbstractEngineInterface.class);
				abstractEngineInterfaces.setHideErrors(log == null);
				if (!abstractEngineInterfaces.isHideErrors()) {
					abstractEngineInterfaces.setErrorListener((serviceClass, t) -> log.error(
						"Failed to initialize the EngineInterface class {}", serviceClass.getCanonicalName(), t));
				}
				final Map<String, String> engineAliases = new TreeMap<>();
				final Map<String, AbstractEngineInterface> interfaces = new TreeMap<>();
				abstractEngineInterfaces.forEach((abstractEngineInterface) -> {
					final String canonicalName = AbstractEngineInterface
						.canonicalizeName(abstractEngineInterface.getName());
					if (interfaces.containsKey(canonicalName)) {
						AbstractEngineInterface oldInterface = interfaces.get(canonicalName);
						String msg = String.format(
							"EngineInterface title conflict on canonical title [%s] between classes%n\t[%s]=[%s]%n\t[%s]=[%s]",
							canonicalName, oldInterface.getClass().getCanonicalName(), oldInterface.getName(),
							abstractEngineInterface.getClass().getCanonicalName(), abstractEngineInterface.getName());
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
				});

				if (interfaces.isEmpty()) {
					AbstractEngineInterface.INTERFACES = Collections.emptyMap();
					AbstractEngineInterface.ENGINE_ALIASES = Collections.emptyMap();
					log.warn("No engine interfaces were located. Please check the contents of the service file for {}",
						AbstractEngineInterface.class.getCanonicalName());
				} else {
					AbstractEngineInterface.INTERFACES = Tools.freezeMap(new LinkedHashMap<>(interfaces));
					AbstractEngineInterface.ENGINE_ALIASES = Tools.freezeMap(new LinkedHashMap<>(engineAliases));
				}
			});
	}

	public static AbstractEngineInterface get(final Logger log, final String engine) {
		if (StringUtils.isEmpty(engine)) {
			throw new IllegalArgumentException("Must provide a non-empty engine title");
		}

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

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final String name;
	private final Set<String> aliases;

	protected AbstractEngineInterface(String name) {
		this(name, null);
	}

	protected AbstractEngineInterface(String name, Set<String> aliases) {
		name = StringUtils.strip(name);
		if (StringUtils.isBlank(name)) {
			throw new IllegalArgumentException("Must provide a non-null, non-blank engine name");
		}
		this.name = name;
		if ((aliases == null) || aliases.isEmpty()) {
			this.aliases = Collections.emptySet();
		} else {
			final Set<String> newAliases = aliases.stream().map(StringUtils::strip)
				.filter((a) -> (!StringUtils.isBlank(a) && !this.name.equals(a)))
				.collect(Collectors.toCollection(TreeSet::new));
			this.aliases = Tools.freezeSet(new LinkedHashSet<>(newAliases));
		}
	}

	public final String getName() {
		return this.name;

	}

	public final Set<String> getAliases() {
		return this.aliases;
	}

	final CommandModule<?> getCommandModule(CalienteCommand command) {
		Objects.requireNonNull(command, "Must provide a non-null command");
		switch (command) {
			case COUNT:
				// TODO: add a counter interface
				break;

			case EXPORT:
				ExportEngineFactory<?, ?, ?, ?, ?, ?> exportEngineFactory = getExportEngineFactory();
				if (exportEngineFactory == null) { return null; }
				return newExporter(exportEngineFactory);

			case IMPORT:
				ImportEngineFactory<?, ?, ?, ?, ?, ?> importEngineFactory = getImportEngineFactory();
				if (importEngineFactory == null) { return null; }
				return newImporter(importEngineFactory);

			default:
				break;
		}

		TransferEngineFactory<?, ?, ?, ?, ?, ?, ?, ?, ?> transferEngineFactory = getImportEngineFactory();
		if (transferEngineFactory == null) {
			transferEngineFactory = getExportEngineFactory();
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

	protected abstract ExportEngineFactory<?, ?, ?, ?, ?, ?> getExportEngineFactory();

	protected ImportCommandModule newImporter(ImportEngineFactory<?, ?, ?, ?, ?, ?> engine) {
		return new ImportCommandModule(engine);
	}

	protected abstract ImportEngineFactory<?, ?, ?, ?, ?, ?> getImportEngineFactory();

	public abstract Collection<? extends LaunchClasspathHelper> getClasspathHelpers();
}