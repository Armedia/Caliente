package com.armedia.caliente.engine.dynamic.xml.metadata;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
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
import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.dslocator.DataSourceDescriptor;
import com.armedia.commons.dslocator.DataSourceLocator;
import com.armedia.commons.utilities.CfgTools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "externalMetadataSource.t", propOrder = {
	"settings",
})
public class MetadataSource {

	@XmlTransient
	protected final Logger log = LoggerFactory.getLogger(getClass());

	@XmlElement(name = "setting", required = true)
	protected List<MetadataSourceSetting> settings;

	@XmlAttribute(name = "name", required = true)
	protected String name;

	@XmlTransient
	private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

	@XmlTransient
	private DataSource dataSource = null;

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
				ret.put(name, StrSubstitutor.replaceSystemProperties(value));
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

	public void initialize() throws Exception {
		final Lock lock = this.rwLock.writeLock();
		lock.lock();
		try {
			if (this.dataSource != null) { return; }
			CfgTools cfg = new CfgTools(getSettingsMap());
			for (DataSourceLocator locator : DataSourceLocator.getAllLocatorsFor("pooled")) {
				final DataSourceDescriptor<?> ds;
				try {
					ds = locator.locateDataSource(cfg);
				} catch (Exception e) {
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
		} finally {
			lock.unlock();
		}
	}

	public Connection getConnection() throws SQLException {
		final Lock lock = this.rwLock.readLock();
		lock.lock();
		try {
			if (this.dataSource == null) { throw new IllegalStateException(
				String.format("The datasource [%s] is not yet initialized", this.name)); }
			return this.dataSource.getConnection();
		} finally {
			lock.unlock();
		}
	}

	public void close() {
		final Lock lock = this.rwLock.writeLock();
		lock.lock();
		try {
			if (this.dataSource == null) { return; }
			// TODO: is there any uninitialization we should be doing here?
			this.dataSource = null;
		} finally {
			lock.unlock();
		}
	}
}