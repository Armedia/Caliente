package com.armedia.caliente.cli.launcher.log;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.Tools;

public abstract class LogConfigurator {

	protected static final String DEFAULT_LOG_NAME = "boot";

	private static final String LOGBACK_DETECTOR_CLASS = "ch.qos.logback.classic.ClassicConstants";
	private static final String LOGBACK_CONFIGURATOR_CLASS = "com.armedia.caliente.cli.newlauncher.LogbackConfigurator";

	private static final String LOG4J_DETECTOR_CLASS = "org.slf4j.impl.Log4jMDCAdapter";
	private static final String LOG4J_CONFIGURATOR_CLASS = "com.armedia.caliente.cli.newlauncher.Log4JConfigurator";

	private static final Collection<Pair<String, String>> INITIALIZERS;

	static {
		Collection<Pair<String, String>> initializers = new ArrayList<>();
		initializers.add(Pair.of(LogConfigurator.LOGBACK_DETECTOR_CLASS, LogConfigurator.LOGBACK_CONFIGURATOR_CLASS));
		initializers.add(Pair.of(LogConfigurator.LOG4J_DETECTOR_CLASS, LogConfigurator.LOG4J_CONFIGURATOR_CLASS));
		INITIALIZERS = Tools.freezeCollection(initializers);
	}

	private static final LogConfigurator FALLBACK = new LogConfigurator() {
		@Override
		public Logger initialize() {
			return new BasicLogger(LogConfigurator.DEFAULT_LOG_NAME);
		}
	};

	LogConfigurator() {
	}

	private static LogConfigurator getBootLogInitializer() {

		for (Pair<String, String> p : LogConfigurator.INITIALIZERS) {
			final String detector = p.getLeft();
			final String initializer = p.getRight();
			try {
				Class.forName(detector);
				try {
					Class<?> c = Class.forName(initializer);
					if (c.isAssignableFrom(LogConfigurator.class)) {
						@SuppressWarnings("unchecked")
						Class<LogConfigurator> i = (Class<LogConfigurator>) c;
						return i.newInstance();
					}
				} catch (Exception e) {
					throw new RuntimeException(
						String.format("Failed to initialize the boot log using [%s]", initializer), e);
				}
			} catch (ClassNotFoundException e) {
				// No this one, move on...
			}
		}

		// By default, then, we use Java logging...
		return LogConfigurator.FALLBACK;
	}

	protected final Logger getDefaultLogger() {
		return LoggerFactory.getLogger(LogConfigurator.DEFAULT_LOG_NAME);
	}

	public static Logger getBootLogger() {
		return LogConfigurator.getBootLogInitializer().initialize();
	}

	public abstract Logger initialize();
}