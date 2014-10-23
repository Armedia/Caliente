/**
 *
 */

package com.armedia.cmf.storage.jdbc;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

/**
 * @author diego
 *
 */
public class SpringDataSourceLocator implements DataSourceLocator {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final String SPRING = "spring";

	@Override
	public DataSource locateDataSource(CfgTools settings) throws Throwable {
		String beanName = Tools.toString(settings.getString(Setting.RESOURCE_NAME), true);
		if (beanName == null) { throw new RuntimeException(
			String
			.format(
				"Configuration setting [%s] was not found (or the value was empty) - can't find bean name for the DataSource",
				Setting.RESOURCE_NAME.getLabel())); }

		if (this.log.isDebugEnabled()) {
			this.log.debug("Finding the DataSource bean with name [{}]", beanName);
		}
		DataSource ds = SpringDataSourceHelper.locateDataSource(beanName);
		if (this.log.isDebugEnabled()) {
			this.log.debug("Located the DataSource named [{}]", beanName);
		}
		return ds;
	}

	@Override
	public boolean supportsLocationType(String locationType) {
		return Tools.equals(this.SPRING, locationType);
	}
}