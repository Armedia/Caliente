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
package com.armedia.caliente.engine.dynamic.xml.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.xml.CmfValueTypeAdapter;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.concurrent.BaseShareableLockable;
import com.armedia.commons.utilities.concurrent.MutexAutoLock;
import com.armedia.commons.utilities.concurrent.SharedAutoLock;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "externalMetadataAttributeTypes.t", propOrder = {
	"mappings", "defaultType"
})
public class AttributeTypeMapping extends BaseShareableLockable {

	private static final boolean DEFAULT_CASE_SENSITIVE = true;

	private static class TypeMatcher {
		private final Pattern pattern;
		private final CmfValue.Type type;

		private TypeMatcher(Pattern pattern, CmfValue.Type type) {
			this.pattern = pattern;
			this.type = type;
		}
	}

	@XmlElement(name = "attribute", required = false)
	protected List<MetadataTypeMapping> mappings;

	@XmlElement(name = "default", required = false)
	@XmlJavaTypeAdapter(CmfValueTypeAdapter.class)
	protected CmfValue.Type defaultType;

	@XmlTransient
	protected List<TypeMatcher> matchers = null;

	@XmlTransient
	protected CmfValue.Type activeDefault = null;

	@XmlTransient
	protected Boolean caseSensitive = null;

	public List<MetadataTypeMapping> getMappings() {
		if (this.mappings == null) {
			this.mappings = new ArrayList<>();
		}
		return this.mappings;
	}

	public void initialize(Boolean caseSensitive) {
		shareLockedUpgradable(() -> this.matchers, Objects::isNull, (e) -> {
			this.caseSensitive = Tools.coalesce(caseSensitive, AttributeTypeMapping.DEFAULT_CASE_SENSITIVE);

			this.activeDefault = this.defaultType;
			List<TypeMatcher> matchers = new ArrayList<>();
			for (final MetadataTypeMapping mapping : getMappings()) {
				String key = mapping.name;
				if ((key != null) && (mapping.type != null)) {
					Pattern p = Pattern.compile(mapping.name, this.caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
					matchers.add(new TypeMatcher(p, mapping.type));
				}
			}
			this.matchers = Tools.freezeList(matchers);
		});
	}

	public boolean isCaseSensitive() {
		return shareLocked(() -> this.caseSensitive);
	}

	public CmfValue.Type getDefaultType() {
		return shareLocked(() -> this.defaultType);
	}

	public void setDefaultType(CmfValue.Type value) {
		try (MutexAutoLock lock = mutexAutoLock()) {
			this.defaultType = value;
			close();
			initialize(this.caseSensitive);
		}
	}

	public CmfValue.Type getMappedType(final String sqlName) {
		try (SharedAutoLock lock = sharedAutoLock()) {
			for (TypeMatcher tm : this.matchers) {
				if (tm.pattern.matcher(sqlName).matches()) { return tm.type; }
			}
			return this.activeDefault;
		}
	}

	public void close() {
		shareLockedUpgradable(() -> this.matchers, Objects::nonNull, (e) -> {
			this.activeDefault = null;
			this.caseSensitive = null;
			this.matchers = null;
		});
	}
}