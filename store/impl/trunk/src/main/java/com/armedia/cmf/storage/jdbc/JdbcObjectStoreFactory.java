package com.armedia.cmf.storage.jdbc;

import java.sql.Connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.storage.ObjectStoreFactory;
import com.armedia.cmf.storage.StorageException;
import com.armedia.cmf.storage.xml.StoreConfiguration;
import com.armedia.commons.dslocator.DataSourceLocator;
import com.armedia.commons.utilities.CfgTools;

public class JdbcObjectStoreFactory extends ObjectStoreFactory<Connection, JdbcOperation, JdbcObjectStore> {

	private static final Logger LOG = LoggerFactory.getLogger(JdbcObjectStoreFactory.class);

	public JdbcObjectStoreFactory() {
		super("jdbc");
	}

	@Override
	protected JdbcObjectStore newInstance(StoreConfiguration configuration) throws StorageException {
		// It's either direct, or taken from Spring or JNDI
		CfgTools cfg = new CfgTools(configuration.getEffectiveSettings());
		final String locationType = cfg.getString(Setting.LOCATION_TYPE);
		for (DataSourceLocator locator : DataSourceLocator.getAllLocatorsFor(locationType)) {
			try {
				return new JdbcObjectStore(locator.locateDataSource(cfg), cfg.getBoolean(Setting.UPDATE_SCHEMA));
			} catch (Throwable e) {
				// This one failed...log it, and try the next one
				JdbcObjectStoreFactory.LOG.error(String.format("Failed to initialize the CmsObjectStore %s[%s]",
					configuration.getName(), configuration.getId()), e);
				continue;
			}
		}

		// If we got here, then we have no locator for that datasource, so we simply explode
		throw new StorageException(String.format("Failed to locate a DataSource for building the ObjectStore %s[%s]",
			configuration.getName(), configuration.getId()));
	}
}