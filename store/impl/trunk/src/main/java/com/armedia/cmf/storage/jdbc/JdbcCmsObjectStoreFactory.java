package com.armedia.cmf.storage.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.storage.CmsObjectStore;
import com.armedia.cmf.storage.CmsObjectStoreFactory;
import com.armedia.cmf.storage.CmsStorageException;
import com.armedia.cmf.storage.xml.CmsObjectStoreConfiguration;
import com.armedia.commons.dslocator.DataSourceLocator;
import com.armedia.commons.utilities.CfgTools;

public class JdbcCmsObjectStoreFactory extends CmsObjectStoreFactory {

	private static final Logger LOG = LoggerFactory.getLogger(JdbcCmsObjectStoreFactory.class);

	public JdbcCmsObjectStoreFactory() {
		super(JdbcCmsObjectStore.class);
	}

	@Override
	protected CmsObjectStore newInstance(CmsObjectStoreConfiguration configuration) throws CmsStorageException {
		// TODO: Define the data store from the configuration...
		// It's either direct, or taken from Spring or JNDI
		CfgTools cfg = new CfgTools(configuration.getEffectiveSettings());
		final String locationType = cfg.getString("location.type");
		for (DataSourceLocator locator : DataSourceLocator.getAllLocatorsFor(locationType)) {
			try {
				return new JdbcCmsObjectStore(locator.locateDataSource(cfg), cfg.getBoolean(Setting.UPDATE_SCHEMA));
			} catch (Throwable e) {
				// This one failed...log it, and try the next one
				JdbcCmsObjectStoreFactory.LOG.error(
					String.format("Failed to initialize the CmsObjectStore with ID=[%s], Class=[%s]",
						configuration.getId(), configuration.getClassName()), e);
				continue;
			}
		}

		// If we got here, then we have no locator for that datasource, so we simply explode
		throw new CmsStorageException(String.format(
			"Failed to locate a DataSource for building the ObjectStore %s[%s]", configuration.getClassName(),
			configuration.getId()));
	}
}