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
 * @author diego
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
		return new DataSourceDescriptor<DataSource>(ds, true);
	}

	@Override
	public boolean supportsLocationType(String locationType) {
		return StringUtils.equalsIgnoreCase(JndiDataSourceLocator.JNDI, locationType);
	}
}