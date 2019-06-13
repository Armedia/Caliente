package com.armedia.caliente.tools;

import java.util.Map;

import org.apache.commons.text.StringSubstitutor;

public class CLIConst {

	public static final String DEFAULT_LOG_FORMAT = "caliente-dctm-ticket-decoder-${logTimeStamp}";

	public static final String getLogName(Map<String, ?> properties) {
		return StringSubstitutor.replace(CLIConst.DEFAULT_LOG_FORMAT, properties);
	}

	public static final String getLogName() {
		return StringSubstitutor.replaceSystemProperties(CLIConst.DEFAULT_LOG_FORMAT);
	}
}