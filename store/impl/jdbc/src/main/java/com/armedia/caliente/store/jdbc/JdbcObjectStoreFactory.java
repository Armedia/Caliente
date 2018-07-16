package com.armedia.caliente.store.jdbc;

import java.sql.Connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.store.CmfObjectStoreFactory;
import com.armedia.caliente.store.CmfPrepInfo;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.xml.StoreConfiguration;
import com.armedia.commons.dslocator.DataSourceDescriptor;
import com.armedia.commons.dslocator.DataSourceLocator;
import com.armedia.commons.utilities.CfgTools;

public class JdbcObjectStoreFactory extends CmfObjectStoreFactory<Connection, JdbcOperation, JdbcObjectStore> {

	private static final Logger LOG = LoggerFactory.getLogger(JdbcObjectStoreFactory.class);

	public JdbcObjectStoreFactory() {
		super("jdbc");
	}

	@Override
	protected JdbcObjectStore newInstance(StoreConfiguration configuration, boolean cleanData, CmfPrepInfo prepInfo)
		throws CmfStorageException {
		// It's either direct, or taken from Spring or JNDI
		CfgTools cfg = new CfgTools(configuration.getEffectiveSettings());
		final String locationType = cfg.getString(Setting.LOCATION_TYPE);
		for (DataSourceLocator locator : DataSourceLocator.getAllLocatorsFor(locationType)) {
			final DataSourceDescriptor<?> ds;
			try {
				ds = locator.locateDataSource(cfg);
			} catch (Exception e) {
				if (JdbcObjectStoreFactory.LOG.isTraceEnabled()) {
					JdbcObjectStoreFactory.LOG.warn(String.format(
						"Exception caught attempting to locate a DataSource via %s for CmfObjectStore %s[%s]",
						locator.getClass().getCanonicalName(), configuration.getType(), configuration.getId()), e);
				}
				continue;
			}
			try {
				return new JdbcObjectStore(ds, cfg.getBoolean(Setting.UPDATE_SCHEMA), cleanData, cfg);
			} catch (Exception e) {
				throw new CmfStorageException(String.format("Failed to initialize the CmsObjectStore %s[%s]",
					configuration.getType(), configuration.getId()), e);
			}
		}

		// If we got here, then we have no locator for that datasource, so we simply explode
		throw new CmfStorageException(
			String.format("Failed to locate a DataSource for building the CmfObjectStore %s[%s]",
				configuration.getType(), configuration.getId()));
	}
}