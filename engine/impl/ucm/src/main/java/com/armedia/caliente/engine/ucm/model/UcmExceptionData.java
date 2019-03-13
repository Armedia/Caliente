package com.armedia.caliente.engine.ucm.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.function.LazySupplier;

public class UcmExceptionData {

	public static final class Entry {
		private final String tag;
		private final List<String> parameters;
		private final List<String> allValues;
		private final LazySupplier<String> string;

		private Entry(List<String> parameters) {
			this.allValues = Tools.freezeCopy(parameters);
			this.tag = parameters.remove(0);
			this.parameters = Tools.freezeList(parameters);
			this.string = new LazySupplier<>(() -> Tools.joinEscaped(',', this.allValues));
		}

		public String getTag() {
			return this.tag;
		}

		public boolean tagIs(String candidate) {
			return Tools.equals(candidate, this.tag);
		}

		public List<String> getParameters() {
			return this.parameters;
		}

		@Override
		public String toString() {
			return this.string.get();
		}
	}

	private static List<String> parseParameters(String msg) {
		return Tools.splitEscaped(',', msg);
	}

	public static List<Entry> parseMessageKey(String msg) {
		if (msg == null) { return Collections.emptyList(); }
		List<Entry> data = new ArrayList<>();
		for (String part : Tools.splitEscaped('!', msg)) {
			if (!StringUtils.isEmpty(part)) {
				List<String> parameters = UcmExceptionData.parseParameters(part);
				if (!parameters.isEmpty()) {
					data.add(new Entry(parameters));
				}
			}
		}
		return data;
	}

	public static String generateMessageKey(List<Entry> entries) {
		List<String> str = new ArrayList<>(entries.size());
		str.add(""); // This adds the leading '!'
		for (Entry e : entries) {
			str.add(e.toString());
		}
		return Tools.joinEscaped('!', str);
	}
}