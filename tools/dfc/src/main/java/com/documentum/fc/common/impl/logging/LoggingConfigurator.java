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
package com.documentum.fc.common.impl.logging;

import java.io.File;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Level;

import com.documentum.fc.common.DfFileWatcher;
import com.documentum.fc.common.DfLogger;
import com.documentum.fc.common.DfPreferences;
import com.documentum.fc.common.DfUtil;
import com.documentum.fc.common.impl.preferences.IPreferencesObserver;
import com.documentum.fc.common.impl.preferences.TypedPreferences;

public class LoggingConfigurator {
	public static Level getLevelToForceStack() {
		return LoggingConfigurator.s_levelToForceStack.get();
	}

	public static void performInitialConfiguration() {
		// Do nothing... we let other layers initialize the logging
	}

	private static void configureLog4j(URL configuration) {
		if (configuration.toString().contains(".xml")) {
			org.apache.log4j.xml.DOMConfigurator.configure(configuration);
		} else {
			org.apache.log4j.PropertyConfigurator.configure(configuration);
		}
	}

	public static synchronized void onPreferencesInitialized() {
		if (LoggingConfigurator.s_preferenceObserver == null) {
			LoggingConfigurator.s_preferenceObserver = new PreferencesObserver();

			DfLogger.isInfoEnabled(LoggingConfigurator.class);
			if (LoggingConfigurator.s_log4jConfiguration != null) {
				File file = DfUtil.getFileFromUrl(LoggingConfigurator.s_log4jConfiguration);
				if (file != null) {
					DfFileWatcher.access().register(file, new FileObserver());
				}
			}
		}
	}

	private static class PreferencesObserver implements IPreferencesObserver {
		public PreferencesObserver() {
			DfPreferences.getInstance().addObserver(this);
			update(DfPreferences.getInstance(), null);
		}

		@Override
		public void update(TypedPreferences preferences, String preferenceName) {
			LoggingConfigurator.s_levelToForceStack
				.set(Level.toLevel(DfPreferences.getInstance().getLoggingLevelToForceStack(), Level.OFF));
		}
	}

	private static class FileObserver implements com.documentum.fc.common.IDfFileObserver {
		@Override
		public void update(File f) {
			LoggingConfigurator.configureLog4j(LoggingConfigurator.s_log4jConfiguration);
		}
	}

	private static URL s_log4jConfiguration = null;
	private static AtomicReference<Level> s_levelToForceStack = new AtomicReference<>(Level.OFF);
	private static IPreferencesObserver s_preferenceObserver;
}