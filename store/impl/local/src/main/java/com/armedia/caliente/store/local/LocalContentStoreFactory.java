package com.armedia.caliente.store.local;

import java.io.File;

import com.armedia.caliente.store.CmfContentStoreFactory;
import com.armedia.caliente.store.CmfOrganizationStrategy;
import com.armedia.caliente.store.CmfPrepInfo;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.xml.StoreConfiguration;
import com.armedia.commons.utilities.CfgTools;

public class LocalContentStoreFactory extends CmfContentStoreFactory<LocalContentStore> {

	public LocalContentStoreFactory() {
		super("local", "fs");
	}

	@Override
	protected LocalContentStore newInstance(StoreConfiguration configuration, boolean cleanData, CmfPrepInfo prepInfo)
		throws CmfStorageException {
		// It's either direct, or taken from Spring or JNDI
		CfgTools cfg = new CfgTools(configuration.getEffectiveSettings());
		String basePath = cfg.getString(LocalContentStoreSetting.BASE_DIR);
		if (basePath == null) { throw new CmfStorageException(
			String.format("No setting [%s] specified", LocalContentStoreSetting.BASE_DIR.getLabel())); }
		// Resolve system properties

		CmfOrganizationStrategy strategy = CmfOrganizationStrategy
			.getStrategy(cfg.getString(LocalContentStoreSetting.URI_STRATEGY));
		if (this.log.isDebugEnabled()) {
			this.log.debug(String.format("Creating a new local file store with base path [%s], and strategy [%s]",
				basePath, strategy.getName()));
		}
		return new LocalContentStore(cfg, new File(basePath), strategy, cleanData);
	}
}