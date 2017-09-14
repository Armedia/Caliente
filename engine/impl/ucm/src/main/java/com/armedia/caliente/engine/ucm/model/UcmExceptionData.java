package com.armedia.caliente.engine.ucm.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class UcmExceptionData {
	private static final Pattern MSGKEY_EXCL = Pattern.compile("(?<!\\\\)!");
	private static final Pattern MSGKEY_COMMA = Pattern.compile("(?<!\\\\),");

	private static List<String> parseCommas(String msg) {
		int prev = 0;
		List<String> ret = new ArrayList<>();
		Matcher m = UcmExceptionData.MSGKEY_COMMA.matcher(msg);
		while (m.find()) {
			ret.add(msg.substring(prev, m.start()));
			prev = m.end();
		}
		if (prev < msg.length()) {
			ret.add(msg.substring(prev));
		}
		return ret;
	}

	static List<List<String>> parseMessages(String msg) {
		if (msg == null) { return Collections.emptyList(); }
		int prev = -1;
		List<List<String>> data = new ArrayList<>();
		Matcher m = UcmExceptionData.MSGKEY_EXCL.matcher(msg);
		while (m.find()) {
			if (prev >= 0) {
				data.add(UcmExceptionData.parseCommas(msg.substring(prev, m.start())));
			}
			prev = m.end();
		}
		if (prev >= 0) {
			data.add(UcmExceptionData.parseCommas(msg.substring(prev)));
		}
		return data;
	}
}