/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.apache.commons.text.StringEscapeUtils;

import com.armedia.commons.utilities.Tools;

public class CsvFormatter {
	public static final boolean DEFAULT_RENDER_TERMINATOR = false;
	public static final Function<Object, String> DEFAULT_ENCODER = Tools::toString;
	private static final String LF = String.format("%n");

	private final Function<? super Object, String> encoder;
	private final int columns;
	private final List<String> headers;
	private final boolean renderTerminator;

	public CsvFormatter(String... headers) {
		this(CsvFormatter.DEFAULT_RENDER_TERMINATOR, headers);
	}

	public CsvFormatter(boolean renderTerminator, String... headers) {
		this(renderTerminator, Arrays.asList(headers));
	}

	public CsvFormatter(Collection<String> headers) {
		this(CsvFormatter.DEFAULT_RENDER_TERMINATOR, headers);
	}

	public CsvFormatter(boolean renderTerminator, Collection<String> headers) {
		this(CsvFormatter.DEFAULT_ENCODER, renderTerminator, headers);
	}

	public CsvFormatter(Function<? super Object, String> encoder, String... headers) {
		this(encoder, CsvFormatter.DEFAULT_RENDER_TERMINATOR, headers);
	}

	public CsvFormatter(Function<? super Object, String> encoder, boolean renderTerminator, String... headers) {
		this(encoder, renderTerminator, Arrays.asList(headers));
	}

	public CsvFormatter(Function<? super Object, String> encoder, Collection<String> headers) {
		this(encoder, CsvFormatter.DEFAULT_RENDER_TERMINATOR, headers);
	}

	public CsvFormatter(Function<? super Object, String> encoder, boolean renderTerminator,
		Collection<String> headers) {
		this.encoder = Objects.requireNonNull(encoder, "Must provide a non-null encoder");
		Objects.requireNonNull(headers, "Must provide a collection of header strings");
		if (headers.isEmpty()) { throw new IllegalArgumentException("Header collection may not be empty"); }
		this.headers = Tools.freezeList(new ArrayList<>(headers));
		this.columns = headers.size();
		this.renderTerminator = renderTerminator;
	}

	public final Function<Object, String> getEncoder() {
		return this.encoder;
	}

	public final boolean isRenderTerminator() {
		return this.renderTerminator;
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
			String str = this.encoder.apply(o);
			if (str == null) {
				str = "";
			} else {
				str = StringEscapeUtils.escapeCsv(str);
			}
			if (!first) {
				sb.append(',');
			}
			sb.append(str);
			first = false;
		}
		if (this.renderTerminator) {
			sb.append(CsvFormatter.LF);
		}
		return sb.toString();
	}
}