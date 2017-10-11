package com.armedia.caliente.engine.dynamic.xml.extmeta;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
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
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;
import com.armedia.commons.dslocator.DataSourceDescriptor;
import com.armedia.commons.dslocator.DataSourceLocator;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "externalMetadataSource.t", propOrder = {
	"settings", "loaders"
})
public class MetadataSource {

	@XmlTransient
	protected final Logger log = LoggerFactory.getLogger(getClass());

	@XmlElementWrapper(name = "settings")
	@XmlElement(name = "setting", required = true)
	protected List<MetadataSourceSetting> settings;

	@XmlElements({
		@XmlElement(name = "from-sql", type = MetadataFromSQL.class),
		@XmlElement(name = "from-ddl", type = MetadataFromDDL.class)
	})
	protected List<AttributeValuesLoader> loaders;

	@XmlAttribute(name = "id", required = true)
	protected String id;

	@XmlAttribute(name = "failOnError", required = false)
	protected Boolean failOnError;

	@XmlAttribute(name = "failOnMissing", required = false)
	protected Boolean failOnMissing;

	@XmlTransient
	private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

	@XmlTransient
	private List<AttributeValuesLoader> initializedLoaders;

	@XmlTransient
	private DataSource dataSource = null;

	public List<AttributeValuesLoader> getLoaders() {
		if (this.loaders == null) {
			this.loaders = new ArrayList<>();
		}
		return this.loaders;
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
				ret.put(name, StrSubstitutor.replaceSystemProperties(value));
			}
		}
		return ret;
	}

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public boolean isFailOnError() {
		return Tools.coalesce(this.failOnError, Boolean.FALSE);
	}

	public void setFailOnError(Boolean value) {
		this.failOnError = value;
	}

	public boolean isFailOnMissing() {
		return Tools.coalesce(this.failOnMissing, Boolean.FALSE);
	}

	public void setFailOnMissing(Boolean value) {
		this.failOnMissing = value;
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

				List<AttributeValuesLoader> initializedLoaders = new ArrayList<>();
				try (final Connection c = dataSource.getConnection()) {
					for (AttributeValuesLoader loader : getLoaders()) {
						if (loader != null) {
							loader.initialize(c);
							initializedLoaders.add(loader);
						}
					}
				} finally {
					if (!initializedLoaders.isEmpty()) {
						this.initializedLoaders = initializedLoaders;
					}
				}

				this.dataSource = dataSource;
				return;
			}
			throw new Exception("Failed to initialize this metadata source - no datasources located!");
		} finally {
			lock.unlock();
		}
	}

	public <V> Map<String, CmfAttribute<V>> getAttributeValues(CmfObject<V> object) throws Exception {
		final Lock lock = this.rwLock.readLock();
		lock.lock();
		try {
			// If there are no loades initialized, this is a problem...
			if (this.initializedLoaders == null) { throw new Exception("This metadata source is not yet initialized"); }

			// If there are no loaders initialized, we always return empty
			if (this.initializedLoaders.isEmpty()) { return null; }

			Map<String, CmfAttribute<V>> finalAttributes = new HashMap<>();
			final Connection c;
			try {
				c = this.dataSource.getConnection();
			} catch (SQLException e) {
				if (isFailOnError()) {
					// An exceptikon was caught, but we need to fail on it
					throw new Exception(String.format(
						"Failed to obtain a JDBC connection for loading external metadata attributes for %s",
						object.getDescription()), e);
				}
				// If we're not supposed to fail on an error, then we simply return null - i.e.
				// nothing
				// found
				return null;
			}

			try {
				for (AttributeValuesLoader l : this.initializedLoaders) {
					if (l != null) {
						Map<String, CmfAttribute<V>> newAttributes = null;
						try {
							newAttributes = l.getAttributeValues(c, object);
						} catch (Exception e) {
							if (isFailOnError()) {
								// An exceptikon was caught, but we need to fail on it
								throw new Exception(
									String.format("Exception raised while loading external metadata attributes for %s",
										object.getDescription()),
									e);
							} else {
								// TODO: Log this exception anyway...
							}
						}

						if ((newAttributes == null) && isFailOnMissing()) {
							// The attribute values are required, but none were found...this is an
							// error!
							throw new Exception(
								String.format("Did not find the required external metadata attributes for %s",
									object.getDescription()));
						}

						if (newAttributes != null) {
							finalAttributes.putAll(newAttributes);
						}
					}
				}
				if (finalAttributes.isEmpty()) {
					finalAttributes = null;
				}
				return finalAttributes;
			} finally {
				DbUtils.closeQuietly(c);
			}
		} finally {
			lock.unlock();
		}
	}

	public void close() {
		final Lock lock = this.rwLock.writeLock();
		lock.lock();
		try {
			if (this.initializedLoaders != null) {
				for (AttributeValuesLoader loader : this.initializedLoaders) {
					try {
						loader.close();
					} catch (Throwable t) {
						this.log.error("Exception caught closing an attribute loader", t);
					}
				}
				this.initializedLoaders = null;
			}
			if (this.dataSource == null) { return; }
			this.dataSource = null;
		} finally {
			lock.unlock();
		}
	}
}