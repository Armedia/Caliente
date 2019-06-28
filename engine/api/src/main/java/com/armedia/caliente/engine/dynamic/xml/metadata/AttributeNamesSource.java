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
package com.armedia.caliente.engine.dynamic.xml.metadata;

import java.sql.Connection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlValue;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.concurrent.BaseShareableLockable;
import com.armedia.commons.utilities.concurrent.SharedAutoLock;

@XmlTransient
public abstract class AttributeNamesSource extends BaseShareableLockable implements Iterable<String> {

	public static final Character DEFAULT_SEPARATOR = Character.valueOf(',');

	@XmlValue
	protected String value;

	@XmlAttribute(name = "caseSensitive")
	protected Boolean caseSensitive;

	@XmlTransient
	private Boolean activeCaseSensitive = null;

	@XmlTransient
	private Map<String, String> values = null;

	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
		this.values = null;
	}

	public boolean isCaseSensitive() {
		return Tools.coalesce(this.caseSensitive, Boolean.FALSE);
	}

	public void setCaseSensitive(Boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}

	protected final String canonicalize(String value) {
		return (this.activeCaseSensitive ? value : StringUtils.upperCase(value));
	}

	protected abstract Set<String> getValues(Connection c) throws Exception;

	public final void initialize(Connection c) throws Exception {
		shareLockedUpgradable(() -> this.values, Objects::isNull, (e) -> {
			this.activeCaseSensitive = isCaseSensitive();
			Set<String> values = getValues(c);
			if (values == null) {
				values = Collections.emptySet();
			}
			Map<String, String> m = new HashMap<>();
			for (String s : values) {
				m.put(canonicalize(s), s);
			}
			this.values = Tools.freezeMap(m);
		});
	}

	public final int size() {
		try (SharedAutoLock lock = autoSharedLock()) {
			if (this.values == null) { return 0; }
			return this.values.size();
		}
	}

	public final Set<String> getValues() {
		try (SharedAutoLock lock = autoSharedLock()) {
			if (this.values == null) { return Collections.emptySet(); }
			return Tools.freezeSet(new LinkedHashSet<>(this.values.values()));
		}
	}

	public final Set<String> getCanonicalizedValues() {
		try (SharedAutoLock lock = autoSharedLock()) {
			if (this.values == null) { return Collections.emptySet(); }
			return Tools.freezeSet(new LinkedHashSet<>(this.values.keySet()));
		}
	}

	public final boolean contains(String str) {
		try (SharedAutoLock lock = autoSharedLock()) {
			if (this.values == null) { return false; }
			return this.values.containsKey(canonicalize(str));
		}
	}

	@Override
	public final Iterator<String> iterator() {
		return getValues().iterator();
	}

	public final void close() {
		shareLockedUpgradable(() -> this.values, Objects::nonNull, (e) -> {
			this.values = null;
			this.activeCaseSensitive = null;
		});
	}
}