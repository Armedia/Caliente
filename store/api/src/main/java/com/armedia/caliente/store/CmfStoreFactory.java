/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
 * %%
 * This file is part of the Caliente software. 
 *  
 * If the software was purchased under a paid Caliente license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *   
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
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

	protected abstract STORE newInstance(CmfStore<?> parent, StoreConfiguration cfg, boolean cleanData,
		Supplier<CfgTools> prepInfo) throws CmfStorageException;

	protected void close() {
	}
}