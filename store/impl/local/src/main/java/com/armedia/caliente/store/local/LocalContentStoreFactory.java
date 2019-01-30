package com.armedia.caliente.store.local;

import java.io.File;
import java.util.function.Supplier;

import com.armedia.caliente.store.CmfContentOrganizer;
import com.armedia.caliente.store.CmfContentStoreFactory;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.xml.StoreConfiguration;
import com.armedia.commons.utilities.CfgTools;

public class LocalContentStoreFactory extends CmfContentStoreFactory<LocalContentStore> {

	public LocalContentStoreFactory() {
		super("local", "fs");
	}

	@Override
	protected LocalContentStore newInstance(StoreConfiguration configuration, boolean cleanData,
		Supplier<CfgTools> prepInfo) throws CmfStorageException {
		// It's either direct, or taken from Spring or JNDI
		CfgTools cfg = new CfgTools(configuration.getEffectiveSettings());
		String basePath = cfg.getString(LocalContentStoreSetting.BASE_DIR);
		if (basePath == null) {
			throw new CmfStorageException(
				String.format("No setting [%s] specified", LocalContentStoreSetting.BASE_DIR.getLabel()));
		}
		// Resolve system properties

		CmfContentOrganizer organizer = CmfContentOrganizer
			.getOrganizer(cfg.getString(LocalContentStoreSetting.URI_ORGANIZER));
		if (this.log.isDebugEnabled()) {
			this.log.debug(String.format("Creating a new local file store with base path [%s], and organizer [%s]",
				basePath, organizer.getName()));
		}
		return new LocalContentStore(cfg, new File(basePath), organizer, cleanData);
	}
}