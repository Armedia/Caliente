package com.armedia.cmf.storage.jdbc;

import javax.sql.DataSource;

import com.armedia.commons.utilities.CfgTools;

public interface DataSourceLocator {

	public boolean supportsLocationType(String locationType);

	public DataSource locateDataSource(CfgTools settings) throws Throwable;
}