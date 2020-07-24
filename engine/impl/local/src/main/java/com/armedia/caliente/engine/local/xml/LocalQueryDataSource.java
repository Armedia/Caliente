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
package com.armedia.caliente.engine.local.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "localQueryDataSource.t", propOrder = {
	"url", "driver", "user", "password", "settings"
})
public class LocalQueryDataSource {

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "setting.t", propOrder = {
		"value"
	})
	public static class Setting {

		@XmlValue
		protected String value;

		@XmlAttribute(name = "name", required = true)
		protected String name;

		public Setting() {

		}

		public Setting(String name, String value) {
			this.name = name;
			this.value = value;
		}

		public String getValue() {
			return this.value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		public String getName() {
			return this.name;
		}

		public void setName(String value) {
			this.name = value;
		}

		@Override
		public String toString() {
			return String.format("Setting [value=%s, name=%s]", this.value, this.name);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.name);
		}

		@Override
		public boolean equals(Object obj) {
			if (!Tools.baseEquals(this, obj)) { return false; }
			Setting other = Setting.class.cast(obj);
			if (!Objects.equals(this.name, other.name)) { return false; }
			if (!Objects.equals(this.value, other.value)) { return false; }
			return true;
		}

	}

	@XmlElement(name = "url", required = true)
	protected String url;

	@XmlElement(name = "driver", required = false)
	protected String driver;

	@XmlElement(name = "user", required = false)
	protected String user;

	@XmlElement(name = "password", required = false)
	protected String password;

	@XmlElement(name = "setting", required = true)
	protected List<Setting> settings;

	@XmlTransient
	protected final Map<String, String> settingsMap = new TreeMap<>();

	@XmlAttribute(name = "name", required = true)
	protected String name;

	protected void afterUnmarshal(Unmarshaller u, Object parent) {
		this.settingsMap.clear();
		if ((this.settings != null) && !this.settings.isEmpty()) {
			this.settings.removeIf((s) -> StringUtils.isEmpty(s.getName()));
			this.settings.forEach((s) -> {
				String k = s.getName();
				String v = s.getValue();
				if (StringUtils.isNotEmpty(k) && (v != null)) {
					this.settingsMap.put(k, v);
				}
			});
		}
	}

	protected void beforeMarshal(Marshaller m) {
		this.settings = new ArrayList<>(this.settingsMap.size());
		this.settingsMap.entrySet().forEach((e) -> {
			Setting s = new Setting(e.getKey(), e.getValue());
			if (StringUtils.isNotEmpty(s.getName()) && (s.getValue() != null)) {
				this.settings.add(s);
			}
		});
	}

	public String getUrl() {
		return this.url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getDriver() {
		return this.driver;
	}

	public void setDriver(String driver) {
		this.driver = driver;
	}

	public String getUser() {
		return this.user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Map<String, String> getSettings() {
		return this.settingsMap;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}
}