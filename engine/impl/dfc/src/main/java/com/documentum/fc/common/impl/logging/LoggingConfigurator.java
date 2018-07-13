package com.documentum.fc.common.impl.logging;

import java.io.File;
import java.net.URL;

import org.apache.log4j.Level;

import com.documentum.fc.common.DfFileWatcher;
import com.documentum.fc.common.DfLoggerDisabled;
import com.documentum.fc.common.DfPreferences;
import com.documentum.fc.common.DfUtil;
import com.documentum.fc.common.impl.preferences.IPreferencesObserver;
import com.documentum.fc.common.impl.preferences.TypedPreferences;

public class LoggingConfigurator {
	public static Level getLevelToForceStack() {
		return LoggingConfigurator.s_levelToForceStack;
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

			DfLoggerDisabled.isInfoEnabled(LoggingConfigurator.class);
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
			LoggingConfigurator.s_levelToForceStack = Level
				.toLevel(DfPreferences.getInstance().getLoggingLevelToForceStack(), Level.OFF);
		}
	}

	private static class FileObserver implements com.documentum.fc.common.IDfFileObserver {
		@Override
		public void update(File f) {
			LoggingConfigurator.configureLog4j(LoggingConfigurator.s_log4jConfiguration);
		}
	}

	private static URL s_log4jConfiguration = null;
	private static Level s_levelToForceStack = Level.OFF;
	private static IPreferencesObserver s_preferenceObserver;
}