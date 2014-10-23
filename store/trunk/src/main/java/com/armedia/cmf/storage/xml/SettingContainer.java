package com.armedia.cmf.storage.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlTransient;

import com.armedia.commons.utilities.Tools;

@XmlTransient
public class SettingContainer {

	@XmlElementWrapper(name = "settings", required = false)
	@XmlElement(name = "setting", required = false)
	private List<Setting> setting;

	@XmlTransient
	private Map<String, String> settings = new LinkedHashMap<String, String>();

	@XmlTransient
	private SettingContainer parent = null;

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
		Map<String, String> m = new HashMap<String, String>();
		Tools.overlayMaps(m, (this.parent != null ? this.parent.getSettings() : null), getSettings());
		return m;
	}
}