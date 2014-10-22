/**
 *
 */

package com.delta.cmsmf.cms.storage.base;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnection;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.log4j.Logger;
import org.h2.tools.DeleteDbFiles;

import com.delta.cmsmf.cfg.Setting;
import com.delta.cmsmf.cms.storage.CmsStorageException;
import com.delta.cmsmf.cms.storage.jdbc.JdbcCmsObjectStore;
import com.delta.cmsmf.exception.CMSMFException;

/**
 * @author diego
 *
 */
public class DefaultCmsObjectStore extends JdbcCmsObjectStore {

	private static final Logger LOG = Logger.getLogger(DefaultCmsObjectStore.class);

	private static class InitData {
		private final DataSource dataSource;
		private final ObjectPool<PoolableConnection> connectionPool;

		private InitData(DataSource dataSource, ObjectPool<PoolableConnection> connectionPool) {
			this.dataSource = dataSource;
			this.connectionPool = connectionPool;
		}
	}

	private static DefaultCmsObjectStore INSTANCE = null;

	public static synchronized DefaultCmsObjectStore init(String jdbcDriver, String jdbcUrl, boolean clearData)
		throws CMSMFException {
		if (DefaultCmsObjectStore.INSTANCE != null) { return DefaultCmsObjectStore.INSTANCE; }
		if (!DbUtils.loadDriver(jdbcDriver)) { throw new CMSMFException(String.format(
			"Failed to locate the JDBC driver class [%s]", jdbcDriver)); }
		if (DefaultCmsObjectStore.LOG.isDebugEnabled()) {
			DefaultCmsObjectStore.LOG.debug(String.format("JDBC driver class [%s] is loaded and valid", jdbcDriver));
		}
		return DefaultCmsObjectStore.init(jdbcUrl, clearData);
	}

	public static synchronized DefaultCmsObjectStore init(String jdbcUrl, boolean clearData) throws CMSMFException {
		if (DefaultCmsObjectStore.INSTANCE != null) { return DefaultCmsObjectStore.INSTANCE; }
		if (DefaultCmsObjectStore.LOG.isInfoEnabled()) {
			DefaultCmsObjectStore.LOG.info(String.format("State database will be stored at [%s]", jdbcUrl));
		}
		final ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(jdbcUrl, null);
		GenericObjectPool<PoolableConnection> connectionPool = new GenericObjectPool<PoolableConnection>();
		GenericObjectPool.Config cfg = new GenericObjectPool.Config();
		cfg.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_GROW;
		cfg.maxIdle = 10;
		cfg.minIdle = 5;
		cfg.testOnBorrow = true;
		cfg.testOnReturn = true;
		connectionPool.setConfig(cfg);
		@SuppressWarnings("unused")
		final PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory,
			connectionPool, null, null, false, true);
		DataSource dataSource = new PoolingDataSource(connectionPool);

		try {
			DefaultCmsObjectStore.INSTANCE = new DefaultCmsObjectStore(new InitData(dataSource, connectionPool),
				clearData);
		} catch (CmsStorageException e) {
			throw new CMSMFException("Failed to initialize the object store", e);
		}
		return DefaultCmsObjectStore.INSTANCE;
	}

	public static synchronized DefaultCmsObjectStore init(boolean clearData) throws CMSMFException {
		if (DefaultCmsObjectStore.INSTANCE != null) { return DefaultCmsObjectStore.INSTANCE; }
		final String driverName = Setting.JDBC_DRIVER.getString();
		final String jdbcUrl = Setting.JDBC_URL.getString();

		String targetPath = Setting.DB_DIRECTORY.getString();
		File targetDirectory = null;
		try {
			targetDirectory = new File(targetPath).getCanonicalFile();
		} catch (IOException e) {
			throw new CMSMFException(String.format("Failed to canonicalize the path [%s]", targetPath), e);
		}

		final String finalUrl = StrSubstitutor.replace(jdbcUrl,
			Collections.singletonMap("target", targetDirectory.getAbsolutePath()));

		// If we're using H2, then we delete the DB if it's in file: protocol
		Pattern p = Pattern.compile("^jdbc:h2:(?:([^:]+):)?(.*)/([^/;]+)(?:;.*)?$");
		Matcher m = p.matcher(finalUrl);
		if (clearData && m.matches()) {
			String protocol = m.group(1);
			if ((protocol == null) || StringUtils.equalsIgnoreCase("file", protocol)) {
				String path = m.group(2);
				String dbName = m.group(3);
				DeleteDbFiles.execute(path, dbName, false);
			}
		}

		// Replace variables in the URL
		return DefaultCmsObjectStore.init(driverName, finalUrl, clearData);
	}

	public static synchronized void close() {
		if (DefaultCmsObjectStore.INSTANCE == null) { return; }
		DefaultCmsObjectStore.INSTANCE.terminate();
		DefaultCmsObjectStore.INSTANCE = null;
	}

	private final ObjectPool<PoolableConnection> connectionPool;
	private final Thread shutdownThread = new Thread() {
		@Override
		public void run() {
			closePool();
		}
	};

	/**
	 * @param dataSource
	 * @param clearData
	 * @throws CMSMFException
	 */
	private DefaultCmsObjectStore(InitData data, boolean clearData) throws CmsStorageException {
		super(data.dataSource, clearData);
		this.connectionPool = data.connectionPool;
		Runtime.getRuntime().addShutdownHook(this.shutdownThread);
	}

	private void closePool() {
		try {
			if (this.log.isInfoEnabled()) {
				this.log.info("Closing the state database connection pool");
			}
			this.connectionPool.close();
		} catch (Exception e) {
			this.log.warn("Failed to close the JDBC connection pool", e);
		}
	}

	public void terminate() {
		try {
			closePool();
		} finally {
			Runtime.getRuntime().removeShutdownHook(this.shutdownThread);
		}
	}
}