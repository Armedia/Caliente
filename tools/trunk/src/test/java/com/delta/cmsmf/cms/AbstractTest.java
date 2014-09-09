package com.delta.cmsmf.cms;

import java.io.IOException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;

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
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;

import com.delta.cmsmf.cms.pool.DctmSessionManager;
import com.delta.cmsmf.utils.ClasspathPatcher;
import com.documentum.com.DfClientX;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;

public abstract class AbstractTest {

	protected final Logger LOG = Logger.getLogger(AbstractTest.class);

	private static final String H2_DRIVER = "org.h2.Driver";
	private static final String H2_URL = "jdbc:h2:mem:cmsmf";

	protected static final ConnectionFactory CONNECTION_FACTORY;

	private static final DctmSessionManager SESSION_MANAGER;

	protected static final ResultSetHandler<Integer> HANDLER_COUNT = new ResultSetHandler<Integer>() {
		@Override
		public Integer handle(ResultSet rs) throws SQLException {
			if (!rs.next()) { return -1; }
			return rs.getInt(1);
		}
	};

	protected static final ResultSetHandler<Boolean> HANDLER_EXISTS = new ResultSetHandler<Boolean>() {
		@Override
		public Boolean handle(ResultSet rs) throws SQLException {
			return rs.next();
		}
	};

	static {
		if (!DbUtils.loadDriver(AbstractTest.H2_DRIVER)) { throw new RuntimeException(String.format(
			"Failed to locate the JDBC driver class [%s]", AbstractTest.H2_DRIVER)); }
		CONNECTION_FACTORY = new DriverManagerConnectionFactory(AbstractTest.H2_URL, null);

		try {
			String userDir = System.getProperty("user.dir");
			System.err.printf("Adding [%s] to the classpath...%n", userDir);
			ClasspathPatcher.addToClassPath(userDir);
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
			AbstractTest.CONNECTION_FACTORY, this.pool, null, null, false, true);
		this.dataSource = new PoolingDataSource(this.pool);
		this.LOG.info("Memory-based connection pool ready");
	}

	@After
	public void tearDown() throws Throwable {
		baseTearDown();
	}

	protected final IDfSession acquireSession() {
		return AbstractTest.SESSION_MANAGER.acquireSession();
	}

	protected final IDfQuery newQuery() {
		return new DfClientX().getQuery();
	}

	protected final void closeQuietly(IDfCollection collection) {
		if (collection == null) { return; }
		try {
			collection.close();
		} catch (DfException e) {
		}
	}

	protected final void releaseSession(IDfSession session) {
		AbstractTest.SESSION_MANAGER.releaseSession(session);
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