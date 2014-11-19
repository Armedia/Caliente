package com.armedia.cmf.storage.xml;

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

import org.apache.commons.lang3.text.StrLookup;
import org.apache.commons.lang3.text.StrSubstitutor;

import com.armedia.commons.utilities.Tools;

@XmlTransient
public class SettingContainer {

	@XmlTransient
	private static class Lookup extends StrLookup<String> {

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
	private Map<String, String> settings = new LinkedHashMap<String, String>();

	@XmlTransient
	private SettingContainer parent = null;

	public SettingContainer() {
	}

	protected SettingContainer(SettingContainer pattern) {
		if (pattern == null) { throw new IllegalArgumentException("Must provide an object to copy state from"); }
		this.setting = new ArrayList<Setting>();
		if (pattern.setting != null) {
			this.setting.addAll(pattern.setting);
		}
		this.settings = new HashMap<String, String>(pattern.settings);
		if (pattern.settings != null) {
			this.settings.putAll(pattern.settings);
		}
		if (pattern.parent != null) {
			this.parent = pattern.parent.clone();
		}
	}

	protected void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
		this.settings = new LinkedHashMap<String, String>();
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
		this.setting = new ArrayList<Setting>(this.settings.size());
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

	@SuppressWarnings("unchecked")
	public final Map<String, String> getEffectiveSettings() {
		final Map<String, String> m = new HashMap<String, String>();
		Tools.overlayMaps(m, (this.parent != null ? this.parent.getEffectiveSettings() : null), getSettings());
		StrSubstitutor sub = new StrSubstitutor(new Lookup(m));
		// We make a copy of the keys to avoid concurrent modification errors
		for (String k : new HashSet<String>(m.keySet())) {
			m.put(k, sub.replace(m.get(k)));
		}
		return m;
	}

	@Override
	public SettingContainer clone() {
		return new SettingContainer(this);
	}
}