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
 *
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