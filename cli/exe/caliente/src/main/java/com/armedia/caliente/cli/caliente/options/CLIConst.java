package com.armedia.caliente.cli.caliente.options;

import java.util.Map;

import org.apache.commons.lang3.text.StrSubstitutor;

public class CLIConst {

	public static final String DEFAULT_LOG_FORMAT = "caliente-${logEngine}-${logMode}-${logTimeStamp}";

	public static final String getLogName(Map<String, ?> properties) {
		return StrSubstitutor.replace(CLIConst.DEFAULT_LOG_FORMAT, properties);
	}

	public static final String getLogName() {
		return StrSubstitutor.replaceSystemProperties(CLIConst.DEFAULT_LOG_FORMAT);
	}
}