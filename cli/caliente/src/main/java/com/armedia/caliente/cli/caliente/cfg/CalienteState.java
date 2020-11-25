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
package com.armedia.caliente.cli.caliente.cfg;

import java.io.File;

import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;

public class CalienteState {

	private final File baseDataLocation;

	private final File objectStoreLocation;
	private final CmfObjectStore<?> objectStore;

	private final File contentStoreLocation;
	private final CmfContentStore<?, ?> contentStore;

	public CalienteState(File baseDataLocation, File objectStoreLocation, CmfObjectStore<?> objectStore,
		File contentStoreLocation, CmfContentStore<?, ?> contentStore) {
		this.baseDataLocation = baseDataLocation;
		this.objectStoreLocation = objectStoreLocation;
		this.objectStore = objectStore;
		this.contentStoreLocation = contentStoreLocation;
		this.contentStore = contentStore;
	}

	public File getBaseDataLocation() {
		return this.baseDataLocation;
	}

	public File getObjectStoreLocation() {
		return this.objectStoreLocation;
	}

	public CmfObjectStore<?> getObjectStore() {
		return this.objectStore;
	}

	public File getContentStoreLocation() {
		return this.contentStoreLocation;
	}

	public CmfContentStore<?, ?> getContentStore() {
		return this.contentStore;
	}
}