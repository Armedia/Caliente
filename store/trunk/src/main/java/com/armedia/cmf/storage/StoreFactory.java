package com.armedia.cmf.storage;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.storage.xml.StoreConfiguration;

public abstract class StoreFactory<S> {

	private static final Pattern VALIDATOR = Pattern.compile("^[a-zA-Z_$][a-zA-Z\\d_$]*$");

	private static final boolean isValidAlias(String alias) {
		return (alias != null) && StoreFactory.VALIDATOR.matcher(alias).matches();
	}

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final Set<String> aliases;

	StoreFactory(String... aliases) {
		this(aliases != null ? Arrays.asList(aliases) : null);
	}

	StoreFactory(Collection<String> aliases) {
		Set<String> a = new TreeSet<String>();
		if (aliases != null) {
			for (String alias : aliases) {
				if (StoreFactory.isValidAlias(alias)) {
					a.add(alias);
				}
			}
		}
		if (a.isEmpty()) {
			String msg = String.format("The final alias set for [%s] is empty - cannot continue", getClass()
				.getCanonicalName());
			this.log.error(msg);
			throw new IllegalArgumentException(msg);
		}
		this.aliases = Collections.unmodifiableSet(a);
		this.log.debug("StoreFactory [{}] will attempt to register for the following aliases: {}", getClass()
			.getCanonicalName(), this.aliases);
	}

	protected final Set<String> getAliases() {
		return this.aliases;
	}

	protected abstract S newInstance(StoreConfiguration cfg) throws StorageException;
}