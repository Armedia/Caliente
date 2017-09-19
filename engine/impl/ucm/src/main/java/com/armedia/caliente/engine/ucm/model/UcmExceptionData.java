package com.armedia.caliente.engine.ucm.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.Tools;

public class UcmExceptionData {

	public static final class ExceptionEntry {
		private final String tag;
		private final List<String> parameters;
		private volatile String string = null;

		private ExceptionEntry(String tag, List<String> parameters) {
			this.tag = tag;
			this.parameters = Tools.freezeList(parameters);
		}

		public String getTag() {
			return this.tag;
		}

		public List<String> getParameters() {
			return this.parameters;
		}

		@Override
		public String toString() {
			if (this.string == null) {
				synchronized (this) {
					if (this.string == null) {
						StringBuilder sb = new StringBuilder();
						sb.append('!').append(UcmExceptionData.escape(this.tag));
						for (String p : this.parameters) {
							sb.append(',').append(UcmExceptionData.escape(p));
						}
						this.string = sb.toString();
					}
				}
			}
			return this.string;
		}
	}

	private static final Pattern MSGKEY_EXCL = Pattern.compile("(?<!\\\\)!");
	private static final Pattern MSGKEY_COMMA = Pattern.compile("(?<!\\\\),");

	private static String unescape(String string) {
		return string //
			.replaceAll("\\\\!", "!") //
			.replaceAll("\\\\,", ",") //
		;
	}

	private static String escape(String string) {
		return string //
			.replaceAll("!", "\\\\!") //
			.replaceAll(",", "\\\\,") //
		;
	}

	private static List<String> parseParameters(String msg) {
		int prev = 0;
		List<String> ret = new ArrayList<>();
		Matcher m = UcmExceptionData.MSGKEY_COMMA.matcher(msg);
		while (m.find()) {
			ret.add(UcmExceptionData.unescape(msg.substring(prev, m.start())));
			prev = m.end();
		}
		if (prev < msg.length()) {
			ret.add(UcmExceptionData.unescape(msg.substring(prev)));
		}
		return ret;
	}

	public static List<ExceptionEntry> parseMessageKey(String msg) {
		if (msg == null) { return Collections.emptyList(); }
		int prev = 0;
		List<ExceptionEntry> data = new ArrayList<>();
		Matcher m = UcmExceptionData.MSGKEY_EXCL.matcher(msg);
		while (m.find()) {
			String part = msg.substring(prev, m.start());
			if (!StringUtils.isEmpty(part)) {
				List<String> parameters = UcmExceptionData.parseParameters(part);
				if (!parameters.isEmpty()) {
					String tag = parameters.remove(0);
					data.add(new ExceptionEntry(tag, parameters));
				}
			}
			prev = m.end();
		}
		if (prev < msg.length()) {
			String part = msg.substring(prev);
			if (!StringUtils.isEmpty(part)) {
				List<String> parameters = UcmExceptionData.parseParameters(part);
				if (!parameters.isEmpty()) {
					String tag = parameters.remove(0);
					data.add(new ExceptionEntry(tag, parameters));
				}
			}
		}
		return data;
	}

	public static String generateMessageKey(List<ExceptionEntry> entries) {
		StringBuilder sb = new StringBuilder();
		for (ExceptionEntry e : entries) {
			sb.append(e.toString());
		}
		return sb.toString();
	}
}