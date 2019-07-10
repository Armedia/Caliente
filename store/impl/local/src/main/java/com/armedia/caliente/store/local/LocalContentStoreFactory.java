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
package com.armedia.caliente.store.local;

import java.io.File;
import java.util.function.Supplier;

import com.armedia.caliente.store.CmfContentOrganizer;
import com.armedia.caliente.store.CmfContentStoreFactory;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfStore;
import com.armedia.caliente.store.xml.StoreConfiguration;
import com.armedia.commons.utilities.CfgTools;

public class LocalContentStoreFactory extends CmfContentStoreFactory<LocalContentStore> {

	public LocalContentStoreFactory() {
		super("local", "fs");
	}

	@Override
	protected LocalContentStore newInstance(CmfStore<?> parent, StoreConfiguration configuration, boolean cleanData,
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
			this.log.debug("Creating a new local file store with base path [{}], and organizer [{}]", basePath,
				organizer.getName());
		}
		return new LocalContentStore(parent, cfg, new File(basePath), organizer, cleanData);
	}
}