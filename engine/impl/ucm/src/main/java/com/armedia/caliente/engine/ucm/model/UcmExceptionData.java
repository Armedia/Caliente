package com.armedia.caliente.engine.ucm.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.Tools;

public class UcmExceptionData {

	public static final class Entry {
		private final String tag;
		private final List<String> parameters;
		private volatile String string = null;

		private Entry(String tag, List<String> parameters) {
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

	private static String escape(String string) {
		return string //
			.replaceAll("!", "\\\\!") //
			.replaceAll(",", "\\\\,") //
		;
	}

	private static List<String> parseParameters(String msg) {
		return Tools.splitEscaped(msg, ',');
	}

	public static List<Entry> parseMessageKey(String msg) {
		if (msg == null) { return Collections.emptyList(); }
		List<Entry> data = new ArrayList<>();
		for (String part : Tools.splitEscaped(msg, '!')) {
			if (!StringUtils.isEmpty(part)) {
				List<String> parameters = UcmExceptionData.parseParameters(part);
				if (!parameters.isEmpty()) {
					String tag = parameters.remove(0);
					data.add(new Entry(tag, parameters));
				}
			}
		}
		return data;
	}

	public static String generateMessageKey(List<Entry> entries) {
		StringBuilder sb = new StringBuilder();
		for (Entry e : entries) {
			sb.append(e.toString());
		}
		return sb.toString();
	}
}