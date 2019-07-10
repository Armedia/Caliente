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
package com.armedia.caliente.tools.datasource;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.PluggableServiceLocator;

public abstract class DataSourceLocator {

	protected final Logger log = LoggerFactory.getLogger(getClass());
	private static final Logger LOG = LoggerFactory.getLogger(DataSourceLocator.class);

	public abstract boolean supportsLocationType(String locationType);

	public abstract DataSourceDescriptor<?> locateDataSource(CfgTools settings) throws Exception;

	private static final PluggableServiceLocator<DataSourceLocator> LOADER = new PluggableServiceLocator<>(
		DataSourceLocator.class);

	static {
		DataSourceLocator.LOADER.setErrorListener((c, e) -> {
			if (DataSourceLocator.LOG.isDebugEnabled()) {
				DataSourceLocator.LOG.warn("Failed to instantiate a DataSourceLocator class", e);
			}
		});
		DataSourceLocator.LOADER.setHideErrors(false);
	}

	public static void clearCaches() {
		DataSourceLocator.LOG.debug("Clearing the DataSourceLocator ServiceLoader caches");
		DataSourceLocator.LOADER.reload();
	}

	public static DataSourceLocator getFirstLocatorFor(final String locationType) {
		DataSourceLocator.LOG.debug("Looking for the first locator to match the location type [{}]", locationType);
		try {
			DataSourceLocator first = DataSourceLocator.LOADER.getFirst((l) -> l.supportsLocationType(locationType));
			DataSourceLocator.LOG.debug("The first locator to match the location type [{}] was of class [{}]",
				locationType, first.getClass().getCanonicalName());
			return first;
		} catch (NoSuchElementException e) {
			// No element found...
			DataSourceLocator.LOG.debug("No locator found to match the location type [{}]", locationType);
			return null;
		}
	}

	public static List<DataSourceLocator> getAllLocatorsFor(final String locationType) {
		DataSourceLocator.LOG.debug("Looking for all the locators to match the location type [{}]", locationType);
		List<DataSourceLocator> ret = new ArrayList<>();
		DataSourceLocator.LOADER.getAll((l) -> l.supportsLocationType(locationType)).forEachRemaining((l) -> {
			DataSourceLocator.LOG.debug("Found a locator to match the location type [{}] of class [{}]", locationType,
				l.getClass().getCanonicalName());
			ret.add(l);
		});
		DataSourceLocator.LOG.debug("Returning {} locators that matched the location type [{}] ", ret.size(),
			locationType);
		return ret;
	}
}