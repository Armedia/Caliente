/**
 *
 */

package com.armedia.caliente.tools.datasource.spring;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.armedia.caliente.tools.datasource.DataSourceDescriptor;
import com.armedia.caliente.tools.datasource.DataSourceLocator;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

/**
 * <p>
 * {@code
 * <bean id="springDataSourceLocator" class=
"com.armedia.commons.dslocator.spring.SpringDataSourceLocator" />
 * }
 * </p>
 *
 * @author diego.rivera@armedia.com
 *
 */
public class SpringDataSourceLocator extends DataSourceLocator implements ApplicationContextAware {

	public static final String SPRING = "spring";

	private ApplicationContext ctx = null;

	@Override
	public void setApplicationContext(ApplicationContext ctx) {
		this.ctx = ctx;
	}

	@Override
	public DataSourceDescriptor<?> locateDataSource(CfgTools settings) throws Exception {
		final String beanName = Tools.toString(settings.getString(SpringSetting.BEAN_NAME), true);
		if (beanName == null) {
			throw new NullPointerException(String.format(
				"Configuration setting [%s] was not found (or the value was empty) - can't find bean name for the DataSource",
				SpringSetting.BEAN_NAME.getLabel()));
		}

		this.log.debug("Finding the DataSource bean with name [{}]", beanName);
		DataSource dataSource = this.ctx.getBean(beanName, DataSource.class);
		this.log.debug("Located the DataSource named [{}] of type {}", beanName,
			dataSource.getClass().getCanonicalName());
		// TODO: How to determine if the transactions are managed or not?
		// For now, assume they're managed
		return new DataSourceDescriptor<>(dataSource, true);
	}

	@Override
	public boolean supportsLocationType(String locationType) {
		return StringUtils.equalsIgnoreCase(SpringDataSourceLocator.SPRING, locationType);
	}
}