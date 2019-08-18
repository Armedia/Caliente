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
	private static final String LOGBACK_CONFIGURATOR_CLASS = "com.armedia.caliente.cli.launcher.log.LogbackConfigurator";

	private static final String LOG4J_DETECTOR_CLASS = "org.slf4j.impl.Log4jMDCAdapter";
	private static final String LOG4J_CONFIGURATOR_CLASS = "com.armedia.caliente.cli.launcher.log.Log4JConfigurator";

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