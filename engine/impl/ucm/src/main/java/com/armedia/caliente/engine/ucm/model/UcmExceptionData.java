package com.armedia.caliente.engine.ucm.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

class UcmExceptionData {
	private static final Pattern MSGKEY_EXCL = Pattern.compile("(?<!\\\\)!");
	private static final Pattern MSGKEY_COMMA = Pattern.compile("(?<!\\\\),");

	private static List<String> parseCommas(String msg) {
		int prev = 0;
		List<String> ret = new ArrayList<>();
		Matcher m = UcmExceptionData.MSGKEY_COMMA.matcher(msg);
		while (m.find()) {
			String part = msg.substring(prev, m.start());
			part = part.replaceAll("\\\\!", "!");
			part = part.replaceAll("\\\\,", ",");
			ret.add(part);
			prev = m.end();
		}
		if (prev < msg.length()) {
			String part = msg.substring(prev);
			part = part.replaceAll("\\\\!", "!");
			part = part.replaceAll("\\\\,", ",");
			ret.add(part);
		}
		return ret;
	}

	static List<List<String>> parseMessages(String msg) {
		if (msg == null) { return Collections.emptyList(); }
		int prev = 0;
		List<List<String>> data = new ArrayList<>();
		Matcher m = UcmExceptionData.MSGKEY_EXCL.matcher(msg);
		while (m.find()) {
			String part = msg.substring(prev, m.start());
			if (!StringUtils.isEmpty(part)) {
				data.add(UcmExceptionData.parseCommas(part));
			}
			prev = m.end();
		}
		if (prev < msg.length()) {
			String part = msg.substring(prev);
			if (!StringUtils.isEmpty(part)) {
				data.add(UcmExceptionData.parseCommas(part));
			}
		}
		return data;
	}
}