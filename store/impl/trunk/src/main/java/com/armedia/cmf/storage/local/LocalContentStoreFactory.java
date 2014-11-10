package com.armedia.cmf.storage.local;

import java.io.File;

import org.apache.commons.lang3.text.StrLookup;
import org.apache.commons.lang3.text.StrSubstitutor;

import com.armedia.cmf.storage.ContentStoreFactory;
import com.armedia.cmf.storage.StorageException;
import com.armedia.cmf.storage.xml.StoreConfiguration;
import com.armedia.commons.utilities.CfgTools;

public class LocalContentStoreFactory extends ContentStoreFactory<LocalContentStore> {

	private final StrSubstitutor substitutor;
	private final String ENV_PREFIX = "ENV.";

	public LocalContentStoreFactory() {
		super("local", "filesystem");
		this.substitutor = new StrSubstitutor(new StrLookup<String>() {
			@Override
			public String lookup(String key) {
				if (key.startsWith(LocalContentStoreFactory.this.ENV_PREFIX)) {
					return System.getenv(key.substring(LocalContentStoreFactory.this.ENV_PREFIX.length()));
				} else {
					return System.getProperty(key);
				}
			}
		});
	}

	@Override
	protected LocalContentStore newInstance(StoreConfiguration configuration) throws StorageException {
		// It's either direct, or taken from Spring or JNDI
		CfgTools cfg = new CfgTools(configuration.getEffectiveSettings());
		String basePath = cfg.getString(Setting.BASE_DIR);
		if (basePath == null) { throw new StorageException(String.format("No setting [%s] specified",
			Setting.BASE_DIR.getLabel())); }
		// Resolve system properties
		String actualPath = this.substitutor.replace(basePath);
		if (this.log.isDebugEnabled()) {
			this.log.debug(String.format("Creating a new local file store with base path [%s], expanded as [%s]",
				basePath, actualPath));
		}
		return new LocalContentStore(new File(actualPath));
	}
}