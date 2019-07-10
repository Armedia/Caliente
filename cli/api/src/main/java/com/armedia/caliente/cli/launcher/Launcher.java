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
package com.armedia.caliente.cli.launcher;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;

import com.armedia.caliente.cli.classpath.ClasspathPatcher;
import com.armedia.caliente.cli.launcher.log.LogConfigurator;
import com.armedia.commons.utilities.PluggableServiceLocator;

public abstract class Launcher {

	static {
		// Make sure this is called as early as possible
		ClasspathPatcher.init();
	}

	public static final Logger BOOT_LOG = LogConfigurator.getBootLogger();

	public static final void main(String... args) throws Throwable {
		// First things first, find the first launcher
		ClassLoader cl = ClasspathPatcher.init();
		Class<AbstractExecutable> launcherClass = AbstractExecutable.class;
		PluggableServiceLocator<AbstractExecutable> loader = new PluggableServiceLocator<>(launcherClass, cl);
		final List<Throwable> exceptions = new ArrayList<>();
		loader.setHideErrors(false);
		loader.setErrorListener((c, e) -> exceptions.add(e));
		List<AbstractExecutable> c = new LinkedList<>();
		loader.getAll().forEachRemaining(c::add);
		final int result;
		if (c.isEmpty()) {
			// KABOOM! No launcher found!
			result = 1;
			Launcher.BOOT_LOG.error("No launcher instances were found");
			if (!exceptions.isEmpty()) {
				Launcher.BOOT_LOG.error("{} matching launchers were found, but failed to load:");
				exceptions.forEach((e) -> Launcher.BOOT_LOG.error("Failed Launcher", e));
			}
		} else {
			AbstractExecutable executable = c.get(0);
			Launcher.BOOT_LOG.debug("The executable is of type {}", executable.getClass().getCanonicalName());
			int ret = 0;
			try {
				ret = executable.execute(args);
			} catch (Throwable t) {
				Launcher.BOOT_LOG.error("Failed to execute from {}", executable.getClass().getCanonicalName(), t);
				ret = 1;
			}
			result = ret;
		}
		Launcher.BOOT_LOG.debug("Exiting with result = {}", result);
		System.exit(result);
	}
}