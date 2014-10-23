/**
 *
 */

package com.armedia.cmf.storage.jdbc;

import java.util.Hashtable;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

/**
 * @author diego
 *
 */
public class JndiDataSourceLocator implements DataSourceLocator {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	static final String JNDI = "jndi";

	private static final String JNDI_PREFIX = "jndi.";

	@Override
	public DataSource locateDataSource(CfgTools settings) throws Throwable {

		String jndiName = Tools.toString(settings.getString(Setting.RESOURCE_NAME), true);
		if (jndiName == null) { throw new RuntimeException(
			String
				.format(
					"Configuration setting [%s] was not found (or the value was empty) - can't find JNDI name for the DataSource",
					Setting.RESOURCE_NAME.getLabel())); }

		if (this.log.isDebugEnabled()) {
			this.log.debug("Performing JNDI lookup for the DataSource named [{}]", jndiName);
		}

		Hashtable<String, String> properties = null;
		for (String s : settings.getSettings()) {
			if (!s.startsWith(JndiDataSourceLocator.JNDI_PREFIX)) {
				continue;
			}
			if (properties == null) {
				properties = new Hashtable<String, String>();
			}
			properties.put(s.substring(JndiDataSourceLocator.JNDI_PREFIX.length()), settings.getString(s));
		}

		if (properties.isEmpty()) {
			properties = null;
		}

		InitialContext ctx = new InitialContext(properties);
		Object obj = ctx.lookup(jndiName);
		if (obj == null) { throw new NullPointerException(String.format(
			"The JNDI name [%s] evaluated to a null reference", jndiName)); }
		if (!(obj instanceof DataSource)) { throw new ClassCastException(String.format(
			"The JNDI name [%s] evaluated to an object of class [%s], which is not a subclass of DataSource", jndiName,
			obj.getClass().getCanonicalName())); }
		DataSource ds = DataSource.class.cast(obj);
		if (this.log.isDebugEnabled()) {
			this.log.debug("Located the DataSource named [{}]", jndiName);
		}
		return ds;
	}

	@Override
	public boolean supportsLocationType(String locationType) {
		return Tools.equals(JndiDataSourceLocator.JNDI, locationType);
	}
}