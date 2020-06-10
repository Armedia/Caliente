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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.sql.DataSource;
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
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.tools.datasource.DataSourceDescriptor;
import com.armedia.caliente.tools.datasource.DataSourceLocator;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.concurrent.BaseShareableLockable;
import com.armedia.commons.utilities.concurrent.MutexAutoLock;
import com.armedia.commons.utilities.concurrent.ShareableMap;
import com.armedia.commons.utilities.concurrent.SharedAutoLock;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "localQueriesDataSource.t", propOrder = {
	"url", "driver", "user", "password", "settings"
})
public class LocalQueryDataSource extends BaseShareableLockable {

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "setting.t", propOrder = {
		"value"
	})
	public static class Setting {

		@XmlValue
		protected String value;

		@XmlAttribute(name = "name", required = true)
		protected String name;

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

	}

	@XmlTransient
	protected final Logger log = LoggerFactory.getLogger(getClass());

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
	protected final ShareableMap<String, String> settingsMap = new ShareableMap<>(new TreeMap<>());

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
			Setting s = new Setting();
			s.setName(e.getKey());
			s.setValue(e.getValue());
			if (StringUtils.isNotEmpty(s.getName()) && (s.getValue() != null)) {
				this.settings.add(s);
			}
		});
	}

	public String getUrl() {
		return shareLocked(() -> this.url);
	}

	public void setUrl(String url) {
		try (MutexAutoLock lock = autoMutexLock()) {
			this.url = url;
		}
	}

	public String getDriver() {
		return shareLocked(() -> this.driver);
	}

	public void setDriver(String driver) {
		try (MutexAutoLock lock = autoMutexLock()) {
			this.driver = driver;
		}
	}

	public String getUser() {
		return shareLocked(() -> this.user);
	}

	public void setUser(String user) {
		try (MutexAutoLock lock = autoMutexLock()) {
			this.user = user;
		}
	}

	public String getPassword() {
		return shareLocked(() -> this.password);
	}

	public void setPassword(String password) {
		try (MutexAutoLock lock = autoMutexLock()) {
			this.password = password;
		}
	}

	public Map<String, String> getSettings() {
		return this.settingsMap;
	}

	protected Map<String, String> buildSettingsMap() {
		try (SharedAutoLock lock = autoSharedLock()) {
			try (SharedAutoLock mapLock = this.settingsMap.autoSharedLock()) {
				Map<String, String> ret = new TreeMap<>();
				for (String name : this.settingsMap.keySet()) {
					String value = this.settingsMap.get(name);
					if ((name != null) && (value != null)) {
						setValue(name, value, ret);
					}
				}
				return ret;
			}
		}
	}

	public String getName() {
		return shareLocked(() -> this.name);
	}

	public void setName(String name) {
		try (MutexAutoLock lock = autoMutexLock()) {
			this.name = name;
		}
	}

	private void setValue(String name, String value, Map<String, String> map) {
		value = StringUtils.strip(value);
		if (!StringUtils.isEmpty(value)) {
			map.put(String.format("jdbc.%s", name), StringSubstitutor.replaceSystemProperties(value));
		}
	}

	public DataSource getInstance() throws SQLException {
		try (SharedAutoLock lock = autoSharedLock()) {
			Map<String, String> settingsMap = buildSettingsMap();
			String url = StringUtils.strip(getUrl());
			if (StringUtils.isEmpty(url)) { throw new SQLException("The JDBC url may not be empty or null"); }
			setValue("url", url, settingsMap);

			setValue("driver", getDriver(), settingsMap);
			setValue("user", getUser(), settingsMap);

			String password = getPassword();
			// TODO: Potentially try to decrypt the password...
			setValue("password", password, settingsMap);

			CfgTools cfg = new CfgTools(settingsMap);
			for (DataSourceLocator locator : DataSourceLocator.getAllLocatorsFor("pooled")) {
				final DataSourceDescriptor<?> desc;
				try {
					desc = locator.locateDataSource(cfg);
				} catch (Exception ex) {
					// This one failed...try the next one
					if (this.log.isDebugEnabled()) {
						this.log.warn("Failed to initialize a candidate datasource", ex);
					}
					continue;
				}

				// We have a winner, so return it
				return desc.getDataSource();
			}
			return null;
		}
	}
}