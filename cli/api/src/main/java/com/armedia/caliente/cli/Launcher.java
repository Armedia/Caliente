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
package com.armedia.caliente.cli;

import java.lang.reflect.Method;
import java.util.ArrayList;
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

	private static final String LAUNCHER_CLASSNAME = "com.armedia.caliente.cli.launcher.AbstractLauncher";

	public static final void main(String... args) throws Throwable {
		// First things first, find the first launcher
		ClassLoader cl = ClasspathPatcher.init();
		Thread.currentThread().setContextClassLoader(cl);
		Class<?> launcherClass = cl.loadClass(Launcher.LAUNCHER_CLASSNAME);
		PluggableServiceLocator<?> launchers = new PluggableServiceLocator<>(launcherClass, cl);
		final List<Throwable> exceptions = new ArrayList<>();
		launchers.setHideErrors(false);
		launchers.setErrorListener((c, e) -> exceptions.add(e));
		Object l = launchers.getFirst();
		final int result;
		if (l != null) {
			Launcher.BOOT_LOG.debug("The launcher is of type {}", l.getClass().getCanonicalName());
			int ret = 0;
			try {
				Method launch = l.getClass().getMethod("launch", String[].class);
				Object a = args;
				Object r = launch.invoke(l, a);
				ret = Integer.class.cast(r).intValue();
			} catch (Throwable t) {
				Launcher.BOOT_LOG.error("Failed to launch from {}", l.getClass().getCanonicalName(), t);
				ret = 1;
			}
			result = ret;
		} else {
			// KABOOM! No launcher found!
			result = 1;
			Launcher.BOOT_LOG.error("No launcher instances were found");
			if (!exceptions.isEmpty()) {
				Launcher.BOOT_LOG.error("{} matching launchers were found, but failed to load:");
				exceptions.forEach((e) -> Launcher.BOOT_LOG.error("Failed Launcher", e));
			}
		}
		Launcher.BOOT_LOG.debug("Exiting with result = {}", result);
		System.exit(result);
	}
}