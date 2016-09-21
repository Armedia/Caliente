package com.armedia.cmf.validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Launcher {
	protected static final String DFC_PROPERTIES_PROP = "dfc.properties.file";

	private static final Logger log = LoggerFactory.getLogger(Launcher.class);

	public static final void main(String... args) {
		System.exit(Launcher.runMain(args));
	}

	private static int runMain(String... args) {
		if (!CLIParam.parse(args)) {
			// If the parameters didn't parse, we fail.
			return 1;
		}

		try {
			return Validator.run();
		} catch (Exception e) {
			Launcher.log.error("Exception caught", e);
			return 1;
		}
	}
}