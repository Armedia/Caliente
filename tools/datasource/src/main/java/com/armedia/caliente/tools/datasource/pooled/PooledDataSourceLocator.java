package com.armedia.caliente.tools.datasource.pooled;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.dbcp2.BasicDataSourceFactory;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.armedia.caliente.tools.datasource.DataSourceLocator;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

public class PooledDataSourceLocator extends DataSourceLocator {

	private static final String JDBC_PREFIX = "jdbc.";

	static final String POOLED = "pooled";

	public static final int DEFAULT_MIN_IDLE = 5;
	public static final int DEFAULT_MAX_IDLE = 10;
	public static final int DEFAULT_MAX_TOTAL = -1;
	public static final boolean DEFAULT_TEST_ON_BORROW = true;
	public static final boolean DEFAULT_TEST_ON_RETURN = true;

	private static final Iterable<Pair<String, Object>> DEFAULT_CONFIGS;
	static {
		List<Pair<String, Object>> l = new LinkedList<>();
		l.add(Pair.of("minIdle", PooledDataSourceLocator.DEFAULT_MIN_IDLE));
		l.add(Pair.of("maxIdle", PooledDataSourceLocator.DEFAULT_MAX_IDLE));
		l.add(Pair.of("testOnBorrow", PooledDataSourceLocator.DEFAULT_TEST_ON_BORROW));
		l.add(Pair.of("testOnReturn", PooledDataSourceLocator.DEFAULT_TEST_ON_RETURN));
		l.add(Pair.of("maxTotal", PooledDataSourceLocator.DEFAULT_MAX_TOTAL));

		DEFAULT_CONFIGS = Tools.freezeList(l);
	}

	@Override
	public PooledDataSourceDescriptor locateDataSource(CfgTools settings) throws Exception {
		// Get the URL, user, password, database, schema, etc... other JDBC settings
		Properties properties = new Properties();
		for (String s : settings.getSettings()) {
			if (!s.startsWith(PooledDataSourceLocator.JDBC_PREFIX)) {
				continue;
			}
			String value = settings.getString(s);
			if (value != null) {
				properties.put(s.substring(PooledDataSourceLocator.JDBC_PREFIX.length()), value);
			}
		}

		String driver = properties.getProperty("driverClassName");
		if (driver == null) {
			driver = properties.getProperty("driver");
		}
		if (StringUtils.isBlank(driver)) {
			throw new IllegalArgumentException(
				"No driver class name property was found (driverClassName or driver), can't continue");
		}
		String url = properties.getProperty("url");
		if (StringUtils.isBlank(url)) { throw new IllegalArgumentException("No JDBC URL was given, can't continue"); }

		// Make sure the driverClassName property is correctly named
		properties.remove("driver");
		properties.setProperty("driverClassName", driver);

		// Make sure the username property is correctly named
		String user = properties.getProperty("username");
		if (StringUtils.isBlank(user)) {
			user = properties.getProperty("user");
		}
		properties.remove("user");
		if (!StringUtils.isBlank(user)) {
			properties.setProperty("username", user);
		}

		// Now, pull all the other JDBC-related settings, prefixed with "jdbc.", but excluding
		// the primary four
		if (!DbUtils.loadDriver(driver)) {
			throw new ClassNotFoundException(String.format("Failed to load the JDBC Driver class [%s]", driver));
		}

		// Apply the default configurations if and only if they're not already applied
		PooledDataSourceLocator.DEFAULT_CONFIGS.forEach((p) -> {
			if (!properties.containsKey(p.getLeft()) && (p.getRight() != null)) {
				properties.put(p.getLeft(), Tools.toString(p.getRight()));
			}
		});

		return new PooledDataSourceDescriptor(BasicDataSourceFactory.createDataSource(properties), false);
	}

	@Override
	public boolean supportsLocationType(String locationType) {
		return StringUtils.equalsIgnoreCase(PooledDataSourceLocator.POOLED, locationType);
	}
}