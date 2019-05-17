package com.armedia.caliente.store.jdbc;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.store.CmfObjectStoreFactory;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfStore;
import com.armedia.caliente.store.xml.StoreConfiguration;
import com.armedia.caliente.tools.datasource.DataSourceDescriptor;
import com.armedia.caliente.tools.datasource.DataSourceLocator;
import com.armedia.commons.utilities.CfgTools;

public class JdbcObjectStoreFactory extends CmfObjectStoreFactory<JdbcOperation, JdbcObjectStore> {

	private static final Logger LOG = LoggerFactory.getLogger(JdbcObjectStoreFactory.class);

	public JdbcObjectStoreFactory() {
		super("jdbc");
	}

	@Override
	protected JdbcObjectStore newInstance(CmfStore<?> parent, StoreConfiguration configuration, boolean cleanData,
		Supplier<CfgTools> prepInfo) throws CmfStorageException {
		// It's either direct, or taken from Spring or JNDI
		CfgTools cfg = new CfgTools(configuration.getEffectiveSettings());
		final String locationType = cfg.getString(Setting.LOCATION_TYPE);
		for (DataSourceLocator locator : DataSourceLocator.getAllLocatorsFor(locationType)) {
			final DataSourceDescriptor<?> ds;
			try {
				ds = locator.locateDataSource(cfg);
			} catch (Exception e) {
				if (JdbcObjectStoreFactory.LOG.isTraceEnabled()) {
					JdbcObjectStoreFactory.LOG.warn(
						"Exception caught attempting to locate a DataSource via {} for CmfObjectStore {}[{}]",
						locator.getClass().getCanonicalName(), configuration.getType(), configuration.getId(), e);
				}
				continue;
			}
			try {
				return new JdbcObjectStore(parent, ds, cfg.getBoolean(Setting.UPDATE_SCHEMA), cleanData, cfg);
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