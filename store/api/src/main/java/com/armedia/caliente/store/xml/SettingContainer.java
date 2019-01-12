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