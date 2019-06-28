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
package com.armedia.caliente.store.jdbc;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.store.CmfObjectStoreFactory;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfStore;
import com.armedia.caliente.store.xml.StoreConfiguration;
import com.armedia.caliente.tools.datasource.DataSourceDescriptor;
import com.armedia.caliente.tools.datasource.DataSourceLocator;
import com.armedia.commons.utilities.CfgTools;

public class JdbcObjectStoreFactory extends CmfObjectStoreFactory<JdbcOperation, JdbcObjectStore> {

	private static final Logger LOG = LoggerFactory.getLogger(JdbcObjectStoreFactory.class);

	public JdbcObjectStoreFactory() {
		super("jdbc");
	}

	@Override
	protected JdbcObjectStore newInstance(CmfStore<?> parent, StoreConfiguration configuration, boolean cleanData,
		Supplier<CfgTools> prepInfo) throws CmfStorageException {
		// It's either direct, or taken from Spring or JNDI
		CfgTools cfg = new CfgTools(configuration.getEffectiveSettings());
		final String locationType = cfg.getString(Setting.LOCATION_TYPE);
		for (DataSourceLocator locator : DataSourceLocator.getAllLocatorsFor(locationType)) {
			final DataSourceDescriptor<?> ds;
			try {
				ds = locator.locateDataSource(cfg);
			} catch (Exception e) {
				if (JdbcObjectStoreFactory.LOG.isTraceEnabled()) {
					JdbcObjectStoreFactory.LOG.warn(
						"Exception caught attempting to locate a DataSource via {} for CmfObjectStore {}[{}]",
						locator.getClass().getCanonicalName(), configuration.getType(), configuration.getId(), e);
				}
				continue;
			}
			try {
				return new JdbcObjectStore(parent, ds, cfg.getBoolean(Setting.UPDATE_SCHEMA), cleanData, cfg);
			} catch (Exception e) {
				throw new CmfStorageException(String.format("Failed to initialize the CmsObjectStore %s[%s]",
					configuration.getType(), configuration.getId()), e);
			}
		}

		// If we got here, then we have no locator for that datasource, so we simply explode
		throw new CmfStorageException(
			String.format("Failed to locate a DataSource for building the CmfObjectStore %s[%s]",
				configuration.getType(), configuration.getId()));
	}
}