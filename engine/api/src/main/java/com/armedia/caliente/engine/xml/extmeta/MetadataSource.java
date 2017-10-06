package com.armedia.caliente.engine.xml.extmeta;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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

import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;
import com.armedia.commons.dslocator.DataSourceDescriptor;
import com.armedia.commons.dslocator.DataSourceLocator;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "externalMetadataSource.t", propOrder = {
	"settings", "sources"
})
public class MetadataSource {

	@XmlElementWrapper(name = "settings")
	@XmlElement(name = "setting", required = true)
	protected List<MetadataSourceSetting> settings;

	@XmlElements({
		@XmlElement(name = "from-sql", type = MetadataFromSQL.class),
		@XmlElement(name = "from-ddl", type = MetadataFromDDL.class)
	})
	protected List<AttributeValuesLoader> sources;

	@XmlAttribute(name = "failOnError", required = false)
	protected Boolean failOnError;

	@XmlAttribute(name = "failOnMissing", required = false)
	protected Boolean failOnMissing;

	@XmlTransient
	private DataSource dataSource = null;

	public List<AttributeValuesLoader> getSources() {
		if (this.sources == null) {
			this.sources = new ArrayList<>();
		}
		return this.sources;
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
				ret.put(name, value);
			}
		}
		return ret;
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

	public synchronized void initialize() throws Exception {
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
			this.dataSource = ds.getDataSource();
			return;
		}
		throw new Exception("Failed to initialize this metadata source - no datasources located!");
	}

	public <V> Map<String, CmfAttribute<V>> getAttributeValues(CmfObject<V> object) throws Exception {
		Map<String, CmfAttribute<V>> finalAttributes = new HashMap<>();
		final Connection c;
		try {
			c = this.dataSource.getConnection();
		} catch (SQLException e) {
			if (isFailOnError()) {
				// An exceptikon was caught, but we need to fail on it
				throw new Exception(String.format(
					"Failed to obtain a JDBC connection for loading external metadata attributes for %s (%s)[%s]",
					object.getType(), object.getLabel(), object.getId()), e);
			}
			// If we're not supposed to fail on an error, then we simply return null
			return null;
		}

		try {
			for (AttributeValuesLoader l : getSources()) {
				if (l != null) {
					Map<String, CmfAttribute<V>> newAttributes = null;
					try {
						newAttributes = l.getAttributeValues(c, object);
					} catch (Exception e) {
						if (isFailOnError()) {
							// An exceptikon was caught, but we need to fail on it
							throw new Exception(String.format(
								"Exception raised while loading external metadata attributes for %s (%s)[%s]",
								object.getType(), object.getLabel(), object.getId()), e);
						} else {
							// TODO: Log this exception anyway...
						}
					}

					if ((newAttributes == null) && isFailOnMissing()) {
						// The attribute values are required, but none were found...this is an
						// error!
						throw new Exception(
							String.format("Did not find the required external metadata attributes for %s (%s)[%s]",
								object.getType(), object.getLabel(), object.getId()));
					}

					if (newAttributes != null) {
						finalAttributes.putAll(newAttributes);
					}
				}
			}
			return null;
		} finally {
			DbUtils.closeQuietly(c);
		}
	}

	public synchronized void close() {
		if (this.dataSource == null) { return; }
		this.dataSource = null;
	}
}