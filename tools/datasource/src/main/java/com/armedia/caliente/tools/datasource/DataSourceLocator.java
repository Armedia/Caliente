package com.armedia.caliente.tools.datasource;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.CfgTools;

public abstract class DataSourceLocator {

	protected final Logger log = LoggerFactory.getLogger(getClass());
	private static final Logger LOG = LoggerFactory.getLogger(DataSourceLocator.class);

	public abstract boolean supportsLocationType(String locationType);

	public abstract DataSourceDescriptor<?> locateDataSource(CfgTools settings) throws Exception;

	private static final ServiceLoader<DataSourceLocator> LOADER = ServiceLoader.load(DataSourceLocator.class);

	public static void clearCaches() {
		DataSourceLocator.LOG.debug("Clearing the DataSourceLocator ServiceLoader caches");
		DataSourceLocator.LOADER.reload();
	}

	public static DataSourceLocator getFirstLocatorFor(String locationType) {
		DataSourceLocator.LOG.debug("Looking for the first locator to match the location type [{}]", locationType);
		for (DataSourceLocator locator : DataSourceLocator.LOADER) {
			if (locator.supportsLocationType(locationType)) {
				DataSourceLocator.LOG.debug("The first locator to match the location type [{}] was of class [{}]",
					locationType, locator.getClass().getCanonicalName());
				return locator;
			}
		}
		DataSourceLocator.LOG.debug("No locator found to match the location type [{}]", locationType);
		return null;
	}

	public static List<DataSourceLocator> getAllLocatorsFor(String locationType) {
		DataSourceLocator.LOG.debug("Looking for all the locators to match the location type [{}]", locationType);
		List<DataSourceLocator> ret = new ArrayList<>();
		for (DataSourceLocator locator : DataSourceLocator.LOADER) {
			if (locator.supportsLocationType(locationType)) {
				DataSourceLocator.LOG.debug("Found a first locator to match the location type [{}] of class [{}]",
					locationType, locator.getClass().getCanonicalName());
				ret.add(locator);
			}
		}
		DataSourceLocator.LOG.debug("Returning {} locators that matched the location type [{}] ", ret.size(),
			locationType);
		return ret;
	}
}