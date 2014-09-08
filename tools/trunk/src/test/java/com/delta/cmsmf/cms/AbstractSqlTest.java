package com.delta.cmsmf.cms;

import java.io.IOException;
import java.net.URL;

import javax.sql.DataSource;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnection;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;

import com.delta.cmsmf.cms.pool.DctmSessionManager;
import com.delta.cmsmf.utils.ClasspathPatcher;
import com.documentum.fc.client.IDfSession;

public abstract class AbstractSqlTest {

	protected final Logger LOG = Logger.getLogger(AbstractSqlTest.class);

	private static final String H2_DRIVER = "org.h2.Driver";
	private static final String H2_URL = "jdbc:h2:mem:cmsmf";

	protected static final ConnectionFactory CONNECTION_FACTORY;

	private static final DctmSessionManager SESSION_MANAGER;

	static {
		if (!DbUtils.loadDriver(AbstractSqlTest.H2_DRIVER)) { throw new RuntimeException(String.format(
			"Failed to locate the JDBC driver class [%s]", AbstractSqlTest.H2_DRIVER)); }
		CONNECTION_FACTORY = new DriverManagerConnectionFactory(AbstractSqlTest.H2_URL, null);

		try {
			ClasspathPatcher.addToClassPath(System.getProperty("user.dir"));
		} catch (IOException e) {
			throw new RuntimeException("Failed to add the current working directory to the classpath", e);
		}

		URL properties = Thread.currentThread().getContextClassLoader().getResource("test.properties");
		if (properties == null) { throw new RuntimeException("Failed to load test.properties"); }
		Configuration cfg = null;
		try {
			cfg = new PropertiesConfiguration(properties);
		} catch (ConfigurationException e) {
			throw new RuntimeException("Failed to load the properties configuration", e);
		}

		String docbase = cfg.getString("dctm.docbase");
		String username = cfg.getString("dctm.username");
		String password = cfg.getString("dctm.password");
		SESSION_MANAGER = new DctmSessionManager(docbase, username, password);
	}

	private DataSource dataSource = null;
	private ObjectPool<PoolableConnection> pool = null;

	@Before
	public void setUp() throws Throwable {
		baseSetup();
	}

	protected final void baseSetup() {
		System.out.printf("Preparing the memory-based connection pool");
		this.pool = new GenericObjectPool<PoolableConnection>();
		@SuppressWarnings("unused")
		final PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(
			AbstractSqlTest.CONNECTION_FACTORY, this.pool, null, null, false, true);
		this.dataSource = new PoolingDataSource(this.pool);
		this.LOG.info("Memory-based connection pool ready");
	}

	@After
	public void tearDown() throws Throwable {
		baseTearDown();
	}

	protected final IDfSession acquireSession() {
		return AbstractSqlTest.SESSION_MANAGER.acquireSession();
	}

	protected final void releaseSession(IDfSession session) {
		AbstractSqlTest.SESSION_MANAGER.releaseSession(session);
	}

	protected final void baseTearDown() {
		try {
			this.pool.close();
		} catch (Exception e) {
		}
		this.pool = null;
		this.dataSource = null;
		this.LOG.info("Closing the memory-based connection pool");
	}

	protected final DataSource getDataSource() {
		return this.dataSource;
	}
}