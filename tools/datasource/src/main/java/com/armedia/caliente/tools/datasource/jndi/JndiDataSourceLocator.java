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

package com.armedia.caliente.tools.datasource.jndi;

import java.util.Hashtable;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.tools.datasource.DataSourceDescriptor;
import com.armedia.caliente.tools.datasource.DataSourceLocator;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

/**
 *
 *
 */
public class JndiDataSourceLocator extends DataSourceLocator {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	static final String JNDI = "jndi";

	private static final String JNDI_PREFIX = "jndi.";

	@Override
	public DataSourceDescriptor<?> locateDataSource(CfgTools settings) throws Exception {

		String jndiName = Tools.toString(settings.getString(JndiSetting.DATASOURCE_NAME), true);
		if (jndiName == null) {
			throw new NullPointerException(String.format(
				"Configuration setting [%s] was not found (or the value was empty) - can't find JNDI name for the DataSource",
				JndiSetting.DATASOURCE_NAME.getLabel()));
		}

		this.log.debug("Performing JNDI lookup for the DataSource named [{}]", jndiName);

		Hashtable<String, String> properties = null;
		for (String s : settings.getSettings()) {
			if (!s.startsWith(JndiDataSourceLocator.JNDI_PREFIX)) {
				continue;
			}
			if (properties == null) {
				properties = new Hashtable<>();
			}
			properties.put(s.substring(JndiDataSourceLocator.JNDI_PREFIX.length()), settings.getString(s));
		}

		InitialContext ctx = new InitialContext(properties);
		Object obj = ctx.lookup(jndiName);
		if (obj == null) {
			throw new NullPointerException(String.format("The JNDI name [%s] evaluated to a null reference", jndiName));
		}
		if (!(obj instanceof DataSource)) {
			throw new ClassCastException(String.format(
				"The JNDI name [%s] evaluated to an object of class [%s], which is not a subclass of DataSource",
				jndiName, obj.getClass().getCanonicalName()));
		}
		DataSource ds = DataSource.class.cast(obj);
		this.log.debug("Located the DataSource named [{}]", jndiName);
		// TODO: How to determine if the transactions are managed or not?
		// For now, assume they're managed
		return new DataSourceDescriptor<>(ds, true);
	}

	@Override
	public boolean supportsLocationType(String locationType) {
		return StringUtils.equalsIgnoreCase(JndiDataSourceLocator.JNDI, locationType);
	}
}