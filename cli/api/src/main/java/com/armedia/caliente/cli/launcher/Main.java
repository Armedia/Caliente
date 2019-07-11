/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
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
package com.armedia.caliente.cli.launcher;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;

import com.armedia.caliente.cli.classpath.ClasspathPatcher;
import com.armedia.caliente.cli.launcher.log.LogConfigurator;
import com.armedia.commons.utilities.PluggableServiceLocator;

public final class Main {

	static {
		// Make sure this is called as early as possible
		ClasspathPatcher.init();
	}

	private Main() {
		// So we can't instantiate
	}

	public static final Logger BOOT_LOG = LogConfigurator.getBootLogger();

	public static final void main(String... args) {
		// First things first, find the first launcher
		ClassLoader cl = ClasspathPatcher.init();
		Class<AbstractEntrypoint> launcherClass = AbstractEntrypoint.class;
		PluggableServiceLocator<AbstractEntrypoint> loader = new PluggableServiceLocator<>(launcherClass, cl);
		final List<Throwable> exceptions = new ArrayList<>();
		loader.setHideErrors(false);
		loader.setErrorListener((c, e) -> exceptions.add(e));
		Iterator<AbstractEntrypoint> it = loader.getAll();
		final int result;
		if (it.hasNext()) {
			AbstractEntrypoint entrypoint = it.next();
			Main.BOOT_LOG.debug("Using entrypoint for {} (of type {})", entrypoint.getName(),
				entrypoint.getClass().getCanonicalName());
			it.forEachRemaining((e) -> Main.BOOT_LOG.debug("*** IGNORING entrypoint for {} (of type {})", e.getName(),
				e.getClass().getCanonicalName()));
			int ret = 0;
			try {
				ret = entrypoint.execute(args);
			} catch (Throwable t) {
				Main.BOOT_LOG.error("Failed to launch {} from the entrypoint class {}", entrypoint.getName(),
					entrypoint.getClass().getCanonicalName(), t);
				ret = 1;
			}
			result = ret;
		} else {
			// KABOOM! No launcher found!
			result = 1;
			Main.BOOT_LOG.error("No viable entrypoints were found");
			if (!exceptions.isEmpty()) {
				Main.BOOT_LOG.error("{} possible entrypoints were found, but failed to load:", exceptions.size());
				exceptions.forEach((e) -> Main.BOOT_LOG.error("Failed Entrypoint", e));
			}
		}
		Main.BOOT_LOG.debug("Exiting with result = {}", result);
		System.exit(result);
	}
}