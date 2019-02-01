package com.armedia.caliente.store.jdbc;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.store.CmfContentStoreFactory;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.xml.StoreConfiguration;
import com.armedia.commons.dslocator.DataSourceDescriptor;
import com.armedia.commons.dslocator.DataSourceLocator;
import com.armedia.commons.utilities.CfgTools;

public class JdbcContentStoreFactory extends CmfContentStoreFactory<JdbcContentStore> {

	private static final Logger LOG = LoggerFactory.getLogger(JdbcContentStoreFactory.class);

	public JdbcContentStoreFactory() {
		super("jdbc");
	}

	@Override
	protected JdbcContentStore newInstance(StoreConfiguration configuration, boolean cleanData,
		Supplier<CfgTools> prepInfo) throws CmfStorageException {
		// It's either direct, or taken from Spring or JNDI
		CfgTools cfg = new CfgTools(configuration.getEffectiveSettings());
		final String locationType = cfg.getString(Setting.LOCATION_TYPE);
		for (DataSourceLocator locator : DataSourceLocator.getAllLocatorsFor(locationType)) {
			final DataSourceDescriptor<?> ds;
			try {
				ds = locator.locateDataSource(cfg);
			} catch (Exception e) {
				if (JdbcContentStoreFactory.LOG.isTraceEnabled()) {
					JdbcContentStoreFactory.LOG.warn(
						"Exception caught attempting to locate a DataSource via {} for CmfContentStore {}[{}]",
						locator.getClass().getCanonicalName(), configuration.getType(), configuration.getId(), e);
				}
				continue;
			}
			try {
				return new JdbcContentStore(ds, cfg.getBoolean(Setting.UPDATE_SCHEMA), cleanData, cfg);
			} catch (Exception e) {
				throw new CmfStorageException(String.format("Failed to initialize the CmfContentStore %s[%s]",
					configuration.getType(), configuration.getId()), e);
			}
		}

		// If we got here, then we have no locator for that datasource, so we simply explode
		throw new CmfStorageException(
			String.format("Failed to locate a DataSource for building the CmfContentStore %s[%s]",
				configuration.getType(), configuration.getId()));
	}
}