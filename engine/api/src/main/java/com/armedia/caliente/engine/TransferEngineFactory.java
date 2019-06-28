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
package com.armedia.caliente.engine;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.PluggableServiceLocator;

public abstract class TransferEngineFactory< //
	LISTENER extends TransferListener, //
	RESULT extends Enum<RESULT>, //
	EXCEPTION extends TransferException, //
	SESSION, //
	VALUE, //
	CONTEXT extends TransferContext<SESSION, VALUE, CONTEXT_FACTORY>, //
	CONTEXT_FACTORY extends TransferContextFactory<SESSION, VALUE, CONTEXT, ?>, //
	DELEGATE_FACTORY extends TransferDelegateFactory<SESSION, VALUE, CONTEXT, ?>, //
	ENGINE extends TransferEngine<LISTENER, RESULT, EXCEPTION, SESSION, VALUE, CONTEXT, CONTEXT_FACTORY, DELEGATE_FACTORY, ?> //
> {
	private static final Map<String, Map<String, Object>> REGISTRY = new HashMap<>();
	private static final Map<String, PluggableServiceLocator<?>> LOCATORS = new HashMap<>();

	private static synchronized <FACTORY extends TransferEngineFactory<?, ?, ?, ?, ?, ?, ?, ?, ?>> void registerSubclass(
		Class<FACTORY> subclass) {

		final String key = subclass.getCanonicalName();
		Map<String, Object> m = TransferEngineFactory.REGISTRY.get(key);
		if (m != null) { return; }

		m = new HashMap<>();
		PluggableServiceLocator<FACTORY> locator = new PluggableServiceLocator<>(subclass);
		locator.setHideErrors(true);
		for (FACTORY e : locator) {
			boolean empty = true;
			for (String s : e.getTargetNames()) {
				empty = false;
				Object first = m.get(s);
				if (first != null) {
					// Log the error, and skip the dupe
					continue;
				}
				m.put(s, e);
			}
			if (empty) {
				// Log a warning, then continue
			}
		}
		TransferEngineFactory.REGISTRY.put(key, m);
		TransferEngineFactory.LOCATORS.put(key, locator);
	}

	public static synchronized <ENGINE_FACTORY extends TransferEngineFactory<?, ?, ?, ?, ?, ?, ?, ?, ?>> ENGINE_FACTORY getEngineFactory(
		Class<ENGINE_FACTORY> subclass, String targetName) {
		if (subclass == null) { throw new IllegalArgumentException("Must provide a valid engine subclass"); }
		if (StringUtils.isEmpty(targetName)) {
			throw new IllegalArgumentException("Must provide a non-empty, non-null target name");
		}
		TransferEngineFactory.registerSubclass(subclass);
		Map<String, Object> m = TransferEngineFactory.REGISTRY.get(subclass.getCanonicalName());
		if (m == null) { return null; }
		return subclass.cast(m.get(targetName));
	}

	public static synchronized <ENGINE_FACTORY extends TransferEngineFactory<?, ?, ?, ?, ?, ?, ?, ?, ?>> Set<String> getAvailableEngineFactories(
		Class<ENGINE_FACTORY> subclass) {
		if (subclass == null) { throw new IllegalArgumentException("Must provide a valid engine subclass"); }
		TransferEngineFactory.registerSubclass(subclass);
		return new TreeSet<>(TransferEngineFactory.REGISTRY.keySet());
	}

	protected final boolean supportsDuplicateFileNames;
	protected final CmfCrypt crypto;

	public TransferEngineFactory(boolean supportsDuplicateFileNames, CmfCrypt crypto) {
		this.supportsDuplicateFileNames = supportsDuplicateFileNames;
		this.crypto = crypto;
	}

	public abstract ENGINE newInstance(final Logger output, final WarningTracker warningTracker, final File baseData,
		final CmfObjectStore<?> objectStore, final CmfContentStore<?, ?> contentStore, CfgTools settings)
		throws EXCEPTION;

	public final ENGINE newInstance(final Logger output, final WarningTracker warningTracker, final File baseData,
		final CmfObjectStore<?> objectStore, final CmfContentStore<?, ?> contentStore, Map<String, ?> settings)
		throws EXCEPTION {
		if (settings == null) {
			settings = Collections.emptyMap();
		} else {
			settings = new TreeMap<>(settings);
		}
		return newInstance(output, warningTracker, baseData, objectStore, contentStore, new CfgTools(settings));
	}

	protected abstract Set<String> getTargetNames();

	public final boolean isSupportsDuplicateFileNames() {
		return this.supportsDuplicateFileNames;
	}

	public final CmfCrypt getCrypto() {
		return this.crypto;
	}

}