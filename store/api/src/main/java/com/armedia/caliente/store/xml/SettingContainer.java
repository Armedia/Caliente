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
package com.armedia.caliente.store.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;

import com.armedia.commons.utilities.Tools;

@XmlTransient
public class SettingContainer implements Cloneable {

	@XmlTransient
	private static class Lookup implements StringLookup {

		private final Map<String, String> settings;
		private final String envPrefix = "ENV.";
		private final int envPrefixLength = this.envPrefix.length();

		Lookup(Map<String, String> settings) {
			this.settings = settings;
		}

		@Override
		public String lookup(String key) {
			if (this.settings.containsKey(key)) { return this.settings.get(key); }
			if (key.startsWith(this.envPrefix)) {
				return System.getenv(key.substring(this.envPrefixLength));
			} else {
				return System.getProperty(key);
			}
		}

	};

	@XmlElementWrapper(name = "settings", required = false)
	@XmlElement(name = "setting", required = false)
	private List<Setting> setting;

	@XmlTransient
	private Map<String, String> settings = new LinkedHashMap<>();

	@XmlTransient
	private SettingContainer parent = null;

	protected void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
		this.settings = new LinkedHashMap<>();
		if (this.setting != null) {
			for (Setting s : this.setting) {
				this.settings.put(s.getName(), s.getValue());
			}
		}
		if ((parent != null) && (parent instanceof SettingContainer)) {
			this.parent = SettingContainer.class.cast(parent);
		}
	}

	protected void beforeMarshal(Marshaller marshaller) {
		this.setting = new ArrayList<>(this.settings.size());
		for (Map.Entry<String, String> e : this.settings.entrySet()) {
			Setting s = new Setting();
			s.setName(e.getKey());
			s.setValue(e.getValue());
			this.setting.add(s);
		}
	}

	protected final SettingContainer getParent() {
		return this.parent;
	}

	public final Map<String, String> getSettings() {
		return this.settings;
	}

	public final Map<String, String> getEffectiveSettings() {
		final Map<String, String> m = new HashMap<>();
		Tools.overlayMaps(m, getSettings(), (this.parent != null ? this.parent.getEffectiveSettings() : null));
		StringSubstitutor sub = new StringSubstitutor(new Lookup(m));
		// We make a copy of the keys to avoid concurrent modification errors
		for (String k : new HashSet<>(m.keySet())) {
			m.put(k, sub.replace(m.get(k)));
		}
		return m;
	}

	@Override
	public SettingContainer clone() {
		final SettingContainer newClone;
		try {
			newClone = SettingContainer.class.cast(super.clone());
		} catch (CloneNotSupportedException e) {
			// if java.lang.Object isn't cloneable, we're screwed anyway...
			throw new RuntimeException("Can't clone the SettingContainer", e);
		}
		if (this.setting != null) {
			newClone.setting = new ArrayList<>(this.setting);
		}
		if (this.settings != null) {
			newClone.settings = new HashMap<>(this.settings);
		}
		if (this.parent != null) {
			newClone.parent = this.parent.clone();
		}
		return newClone;
	}
}