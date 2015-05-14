package com.armedia.cmf.storage.local;

import java.io.File;

import com.armedia.cmf.storage.ContentStoreFactory;
import com.armedia.cmf.storage.StorageException;
import com.armedia.cmf.storage.OrganizationStrategy;
import com.armedia.cmf.storage.xml.StoreConfiguration;
import com.armedia.commons.utilities.CfgTools;

public class LocalContentStoreFactory extends ContentStoreFactory<LocalContentStore> {

	public LocalContentStoreFactory() {
		super("local", "filesystem");
	}

	@Override
	protected LocalContentStore newInstance(StoreConfiguration configuration, boolean cleanData)
		throws StorageException {
		// It's either direct, or taken from Spring or JNDI
		CfgTools cfg = new CfgTools(configuration.getEffectiveSettings());
		String basePath = cfg.getString(Setting.BASE_DIR);
		if (basePath == null) { throw new StorageException(String.format("No setting [%s] specified",
			Setting.BASE_DIR.getLabel())); }
		// Resolve system properties

		OrganizationStrategy strategy = OrganizationStrategy.getStrategy(cfg.getString(Setting.URI_STRATEGY));
		if (this.log.isDebugEnabled()) {
			this.log.debug(String.format("Creating a new local file store with base path [%s], and strategy [%s]",
				basePath, strategy.getName()));
		}
		return new LocalContentStore(cfg, new File(basePath), strategy, cleanData);
	}
}