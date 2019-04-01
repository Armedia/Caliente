package com.armedia.caliente.engine.dynamic.xml.metadata;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.sql.DataSource;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.dslocator.DataSourceDescriptor;
import com.armedia.commons.dslocator.DataSourceLocator;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.concurrent.ShareableLockable;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "externalMetadataSource.t", propOrder = {
	"url", "driver", "user", "password", "settings",
})
public class MetadataSource implements ShareableLockable {

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
	protected List<MetadataSourceSetting> settings;

	@XmlAttribute(name = "name", required = true)
	protected String name;

	@XmlTransient
	private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

	@XmlTransient
	private DataSource dataSource = null;

	@Override
	public ReadWriteLock getShareableLock() {
		return this.rwLock;
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

	public List<MetadataSourceSetting> getSettings() {
		if (this.settings == null) {
			this.settings = new ArrayList<>();
		}
		return this.settings;
	}

	public Map<String, String> getSettingsMap() {
		Map<String, String> ret = new TreeMap<>();
		for (MetadataSourceSetting s : getSettings()) {
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

	public void initialize() throws Exception {
		shareLockedUpgradable(() -> this.dataSource, Objects::isNull, (e) -> {
			if (this.dataSource != null) { return; }
			Map<String, String> settingsMap = getSettingsMap();

			String url = StringUtils.strip(getUrl());
			if (StringUtils.isEmpty(url)) { throw new Exception("The JDBC url may not be empty or null"); }
			setValue("url", url, settingsMap);

			setValue("driver", getDriver(), settingsMap);
			setValue("user", getUser(), settingsMap);

			String password = StringUtils.strip(getPassword());
			// TODO: Potentially try to decrypt the password...
			setValue("password", password, settingsMap);

			CfgTools cfg = new CfgTools(settingsMap);
			for (DataSourceLocator locator : DataSourceLocator.getAllLocatorsFor("pooled")) {
				final DataSourceDescriptor<?> ds;
				try {
					ds = locator.locateDataSource(cfg);
				} catch (Exception ex) {
					// This one failed...try the next one
					continue;
				}

				// Set the context with the newly-found DataSource
				DataSource dataSource = ds.getDataSource();
				DbUtils.closeQuietly(dataSource.getConnection());
				this.dataSource = dataSource;
				return;
			}
			throw new Exception("Failed to initialize this metadata source - no datasources located!");
		});
	}

	public Connection getConnection() throws SQLException {
		return shareLocked(() -> {
			if (this.dataSource == null) {
				throw new IllegalStateException(String.format("The datasource [%s] is not yet initialized", this.name));
			}
			return this.dataSource.getConnection();
		});
	}

	public void close() {
		shareLockedUpgradable(() -> this.dataSource, Objects::nonNull, (e) -> {
			// TODO: is there any uninitialization we should be doing here?
			this.dataSource = null;
		});
	}
}