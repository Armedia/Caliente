package com.delta.cmsmf.launcher.dctm;

import com.documentum.fc.common.DfLogger;

public class LogInterceptor {
	static void init() {
		// Try to ensure our version of this class is the first one loaded into the JVM...
		// LoggingConfigurator.class.getCanonicalName();
		DfLogger.class.getCanonicalName();
	}
}