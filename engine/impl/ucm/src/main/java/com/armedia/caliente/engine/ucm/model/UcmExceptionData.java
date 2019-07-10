/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
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