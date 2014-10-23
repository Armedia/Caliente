package com.armedia.cmf.storage.jdbc;

import com.armedia.cmf.storage.CmsObjectStore;
import com.armedia.cmf.storage.CmsObjectStoreFactory;
import com.armedia.cmf.storage.CmsStorageException;
import com.armedia.cmf.storage.xml.CmsObjectStoreConfiguration;

public class JdbcCmsObjectStoreFactory extends CmsObjectStoreFactory {

	public JdbcCmsObjectStoreFactory() {
		super(JdbcCmsObjectStore.class);
	}

	@Override
	protected CmsObjectStore newInstance(CmsObjectStoreConfiguration cfg) throws CmsStorageException {
		// TODO: Define the data store from the configuration...

		// TODO: Determine direct transaction control from the configuration
		// either use an existing JTA engine (using a JNDI or Spring lookup),
		// or use direct transactions, or use neither...
		boolean transactional = false;

		return new JdbcCmsObjectStore(null, transactional);
	}
}