package com.armedia.caliente.cli.caliente.utils;

import java.util.regex.Pattern;

public class RegExDB {

	public static final Pattern LOG_LEVEL = Pattern.compile(
		"^(.+)(?:=(OFF|ALL|TRACE|DEBUG|INFO|WARN|ERROR|FATAL|[1-9][0-9]*))(?:,(1|0|t(?:rue)?|f(?:alse)?|y(?:es)|n(?:o)|on|off)?$",
		Pattern.CASE_INSENSITIVE);

}