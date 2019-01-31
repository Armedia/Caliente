package com.armedia.caliente.cli.launcher.log;

import java.net.URL;

import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Log4JConfigurator extends LogConfigurator {

	@Override
	public Logger initialize() {
		// First, find log4j-boot.xml
		URL config = getClass().getResource("log4j.xml");
		if (config == null) {
			throw new RuntimeException("Failed to configure the boot log - no Log4J boot configuration was found");
		}
		DOMConfigurator.configure(config);
		return LoggerFactory.getLogger(LogConfigurator.DEFAULT_LOG_NAME);
	}

}