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
package com.armedia.caliente.cli.launcher.log;

import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

class LogbackConfigurator extends LogConfigurator {

	@Override
	public Logger initialize() {
		URL config = getClass().getResource("logback.xml");
		if (config == null) {
			throw new RuntimeException("Failed to configure the boot log - no Logback boot configuration was found");
		}

		// assume SLF4J is bound to logback in the current environment
		LoggerContext context = LoggerContext.class.cast(LoggerFactory.getILoggerFactory());
		try {
			JoranConfigurator configurator = new JoranConfigurator();
			configurator.setContext(context);

			// Call context.reset() to clear any previous configuration, e.g. default
			// configuration. For multi-step configuration, omit calling context.reset().
			context.reset();

			configurator.doConfigure(config);
		} catch (JoranException je) {
			// StatusPrinter will handle this
		}

		StatusPrinter.printInCaseOfErrorsOrWarnings(context);
		return getDefaultLogger();
	}

}