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
package com.armedia.caliente.engine.exporter.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
	"settingsList"
})
public class SettingsContainer {

	@XmlElementWrapper(name = "settings", required = false)
	@XmlElement(name = "setting", required = false)
	protected List<SettingT> settingsList;

	@XmlTransient
	private Map<String, String> settings = new HashMap<>();

	protected void beforeMarshal(Marshaller m) {
		List<SettingT> settingsList = new ArrayList<>(this.settings.size());
		if (settingsList != null) {
			for (String k : this.settings.keySet()) {
				settingsList.add(new SettingT(k, this.settings.get(k)));
			}
		}
		this.settingsList = settingsList;
	}

	protected void afterUnmarshal(Unmarshaller u, Object parent) {
		Map<String, String> settings = new HashMap<>();
		if (this.settingsList != null) {
			for (SettingT s : this.settingsList) {
				final String k = s.getName();
				final String v = s.getValue();
				if ((k != null) && (v != null)) {
					settings.put(s.getName(), s.getValue());
				}
			}
		}
		this.settings = settings;
	}

	public Map<String, String> getSettings() {
		Map<String, String> settings = this.settings;
		if (settings == null) {
			settings = this.settings = new HashMap<>();
		}
		return settings;
	}
}