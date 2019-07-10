/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
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
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;

import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.concurrent.BaseShareableLockable;
import com.armedia.commons.utilities.concurrent.ShareableMap;

public abstract class TransferState extends BaseShareableLockable {
	public final UUID jobId = UUID.randomUUID();
	public final Logger output;
	public final File baseData;
	public final CmfObjectStore<?> objectStore;
	public final CmfContentStore<?, ?> streamStore;
	public final CfgTools cfg;

	private final Map<String, Object> properties;

	/**
	 * @param output
	 * @param objectStore
	 * @param streamStore
	 * @param settings
	 */
	protected TransferState(Logger output, Path baseData, CmfObjectStore<?> objectStore,
		CmfContentStore<?, ?> streamStore, CfgTools settings) {
		this.output = output;
		this.baseData = baseData.toFile();
		this.objectStore = objectStore;
		this.streamStore = streamStore;
		this.cfg = settings;
		this.properties = new ShareableMap<>(this, new HashMap<>());
	}

	public final boolean setProperty(String property, Object value) {
		if (property == null) { throw new IllegalArgumentException("Must provide a non-null property name"); }
		if (value == null) { throw new IllegalArgumentException("Must provide a non-null value"); }
		Object o = this.properties.put(property, value);
		return (o != null);
	}

	public final Object removeProperty(String property) {
		if (property == null) { throw new IllegalArgumentException("Must provide a non-null property name"); }
		return this.properties.remove(property);
	}

	public final <T> T getProperty(String property) {
		if (property == null) { throw new IllegalArgumentException("Must provide a non-null property name"); }
		try {
			@SuppressWarnings("unchecked")
			T t = (T) this.properties.get(property);
			return t;
		} catch (ClassCastException e) {
			return null;
		}
	}

	public final boolean hasProperty(String property) {
		if (property == null) { throw new IllegalArgumentException("Must provide a non-null property name"); }
		return this.properties.containsKey(property);
	}

	public final void clearProperties() {
		this.properties.clear();
	}

	public final int getPropertyCount() {
		return this.properties.size();
	}

	public final Set<String> getPropertyNames() {
		return new HashSet<>(this.properties.keySet());
	}
}