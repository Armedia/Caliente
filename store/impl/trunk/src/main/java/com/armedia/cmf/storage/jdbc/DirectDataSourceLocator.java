package com.armedia.cmf.storage.jdbc;

import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnection;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

public class DirectDataSourceLocator implements DataSourceLocator {

	private static final Logger LOG = LoggerFactory.getLogger(DirectDataSourceLocator.class);

	private static final String JDBC_PREFIX = "jdbc.";

	static final String DIRECT = "direct";

	@Override
	public DataSource locateDataSource(CfgTools settings) throws Throwable {
		// Get the URL, user, password, database, schema, etc... other JDBC settings
		final String driver = settings.getString(Setting.JDBC_DRIVER);
		final String url = settings.getString(Setting.JDBC_URL);

		// Now, pull all the other JDBC-related settings, prefixed with "jdbc.", but excluding
		// the primary four
		if (!DbUtils.loadDriver(driver)) { throw new RuntimeException(String.format(
			"Failed to load the JDBC Driver class [%s]", driver)); }
		Properties properties = new Properties();
		for (String s : settings.getSettings()) {
			if (!s.startsWith(DirectDataSourceLocator.JDBC_PREFIX)) {
				continue;
			}
			properties.put(s.substring(DirectDataSourceLocator.JDBC_PREFIX.length()), settings.getString(s));
		}

		final ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(url, properties);
		final GenericObjectPool<PoolableConnection> connectionPool = new GenericObjectPool<PoolableConnection>();
		GenericObjectPool.Config cfg = new GenericObjectPool.Config();
		// TODO: also allow pool configuration here
		cfg.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_GROW;
		cfg.maxIdle = 10;
		cfg.minIdle = 5;
		cfg.testOnBorrow = true;
		cfg.testOnReturn = true;
		connectionPool.setConfig(cfg);
		@SuppressWarnings("unused")
		final PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory,
			connectionPool, null, null, false, true);
		final DataSource ret = new PoolingDataSource(connectionPool);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					connectionPool.close();
				} catch (Exception e) {
					// Log the error
					DirectDataSourceLocator.LOG.error(
						String.format("Failed to close the JDBC connection pool for [%s]/[%s]", driver, url), e);
				}
			}
		});
		return ret;
	}

	@Override
	public boolean supportsLocationType(String locationType) {
		return Tools.equals(DirectDataSourceLocator.DIRECT, locationType);
	}
}