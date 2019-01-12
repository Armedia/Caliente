package com.armedia.caliente.cli.caliente.launcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.apache.commons.text.StringEscapeUtils;

import com.armedia.commons.utilities.Tools;

class ManifestFormatter {
	private final int columns;
	private final List<String> headers;

	ManifestFormatter(String... headers) {
		this(Arrays.asList(headers));
	}

	ManifestFormatter(Collection<String> headers) {
		Objects.requireNonNull(headers, "Must provide a collection of header strings");
		if (headers.isEmpty()) { throw new IllegalArgumentException("Header collection must not be empty"); }
		this.headers = Tools.freezeList(new ArrayList<>(headers));
		this.columns = headers.size();
	}

	public final int getColumns() {
		return this.columns;
	}

	public final List<String> getHeaders() {
		return this.headers;
	}

	public final String renderHeaders() {
		return render(this.headers);
	}

	public final String render(Object... data) {
		if (data == null) { return null; }
		return render(Arrays.asList(data));
	}

	public final String render(Collection<?> data) {
		if (data == null) { return null; }
		if (data.size() != this.columns) {
			throw new IllegalArgumentException(
				String.format("Insufficient data: expected %d data columns, but got %d", this.columns, data.size()));
		}
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (Object o : data) {
			String str = Tools.toString(o);
			if (str != null) {
				str = StringEscapeUtils.escapeCsv(str);
			}
			if (!first) {
				sb.append(',');
			}
			sb.append(str);
			first = false;
		}
		return sb.toString();
	}
}