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
package com.armedia.caliente.engine.dynamic.metadata;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.armedia.caliente.store.CmfAttribute;

public class ExternalMetadata<V> {

	private final Map<String, CmfAttribute<V>> attributes = new HashMap<>();
	private final Map<String, String> sources = new HashMap<>();
	private final Map<String, Set<String>> sourceAttributes = new HashMap<>();
	private final Map<String, Long> recordCounts = new HashMap<>();

	public ExternalMetadata() {
	}

	void addValues(String source, long recordCount, Map<String, CmfAttribute<V>> values) {
		if (values == null) {
			values = Collections.emptyMap();
		}
		Set<String> attributes = new HashSet<>();
		this.sourceAttributes.put(source, attributes);
		this.recordCounts.put(source, recordCount);
		for (String att : values.keySet()) {
			attributes.add(att);
			this.sources.put(att, source);
			this.attributes.put(att, values.get(att));
		}
	}

	public Set<String> getAttributeNames() {
		return new TreeSet<>(this.attributes.keySet());
	}

	public String getAttributeSource(String attribute) {
		return this.sources.get(attribute);
	}

	public Set<String> getAttributesFromSource(String name) {
		if (!this.sourceAttributes.containsKey(name)) { return null; }
		return new TreeSet<>(this.sourceAttributes.get(name));
	}

	public CmfAttribute<V> getAttribute(String name) {
		return this.attributes.get(name);
	}

	public boolean hasAttribute(String name) {
		return this.attributes.containsKey(name);
	}

	public int getAttributeCount() {
		return this.attributes.size();
	}

	public Set<String> getSourceNames() {
		return this.recordCounts.keySet();
	}

	public boolean hasSource(String name) {
		return this.recordCounts.containsKey(name);
	}

	public long getSourceRecordCount(String name) {
		Long l = this.recordCounts.get(name);
		if (l == null) { return 0; }
		return l.intValue();
	}
}