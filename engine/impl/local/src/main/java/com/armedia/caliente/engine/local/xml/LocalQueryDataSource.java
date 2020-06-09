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

	@XmlAttribute(name = "name", required = true)
	protected String name;

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

	public List<Setting> getSettings() {
		if (this.settings == null) {
			this.settings = new ArrayList<>();
		}
		return this.settings;
	}

	public Map<String, String> getSettingsMap() {
		Map<String, String> ret = new TreeMap<>();
		for (Setting s : getSettings()) {
			String name = s.getName();
			String value = s.getValue();
			if ((name != null) && (value != null)) {
				ret.put(String.format("jdbc.%s", name), StringSubstitutor.replaceSystemProperties(value));
			}
		}
		return ret;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	private void setValue(String name, String value, Map<String, String> map) {
		value = StringUtils.strip(value);
		if (!StringUtils.isEmpty(value)) {
			map.put(String.format("jdbc.%s", name), StringSubstitutor.replaceSystemProperties(value));
		}
	}

	public DataSource getInstance() throws SQLException {
		try (SharedAutoLock lock = autoSharedLock()) {
			Map<String, String> settingsMap = getSettingsMap();
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

				// Set the context with the newly-found DataSource
				return desc.getDataSource();
			}
			return null;
		}
	}
}