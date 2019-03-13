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