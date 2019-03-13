package com.armedia.caliente.cli.launcher.log;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.Tools;

public abstract class LogConfigurator {

	protected static final String DEFAULT_LOG_NAME = "boot";

	private static final String LOGBACK_DETECTOR_CLASS = "ch.qos.logback.classic.joran.JoranConfigurator";
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
		// We do this manually instead of via Service Discovery because we need to be VERY, VERY
		// strict with regards to which classes are or aren't loaded...
		for (Pair<String, String> p : LogConfigurator.INITIALIZERS) {
			final String detector = p.getLeft();
			final String configurator = p.getRight();
			try {
				// Find the detector class, but don't initialize it...
				Class.forName(detector, false, null);
				try {
					Class<?> c = Class.forName(configurator);
					if (c.isAssignableFrom(LogConfigurator.class)) {
						return LogConfigurator.class.cast(c.newInstance());
					}
				} catch (Exception e) {
					throw new RuntimeException(
						String.format("Failed to configure the boot log using [%s]", configurator), e);
				}
			} catch (ClassNotFoundException e) {
				// Not this one, move on...
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