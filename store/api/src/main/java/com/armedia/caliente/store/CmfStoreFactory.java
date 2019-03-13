package com.armedia.caliente.store;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.store.xml.StoreConfiguration;
import com.armedia.commons.utilities.CfgTools;

public abstract class CmfStoreFactory<STORE extends CmfStore<?>> {

	public static final String CFG_CLEAN_DATA = "clean.data";

	private static final Pattern VALIDATOR = Pattern.compile("^[a-zA-Z_$][a-zA-Z\\d_$]*$");

	private static final boolean isValidAlias(String alias) {
		return (alias != null) && CmfStoreFactory.VALIDATOR.matcher(alias).matches();
	}

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final Set<String> aliases;

	CmfStoreFactory(String... aliases) {
		this(aliases != null ? Arrays.asList(aliases) : null);
	}

	CmfStoreFactory(Collection<String> aliases) {
		Set<String> a = new TreeSet<>();
		if (aliases != null) {
			for (String alias : aliases) {
				if (CmfStoreFactory.isValidAlias(alias)) {
					a.add(alias);
				}
			}
		}
		if (a.isEmpty()) {
			String msg = String.format("The final alias set for [%s] is empty - cannot continue",
				getClass().getCanonicalName());
			this.log.error(msg);
			throw new IllegalArgumentException(msg);
		}
		this.aliases = Collections.unmodifiableSet(a);
		this.log.debug("CmfStoreFactory [{}] will attempt to register for the following aliases: {}",
			getClass().getCanonicalName(), this.aliases);
	}

	protected final Set<String> getAliases() {
		return this.aliases;
	}

	protected abstract STORE newInstance(StoreConfiguration cfg, boolean cleanData, Supplier<CfgTools> prepInfo)
		throws CmfStorageException;

	protected void close() {
	}
}