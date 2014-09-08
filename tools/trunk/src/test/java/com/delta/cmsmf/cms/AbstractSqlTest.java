package com.delta.cmsmf.cms;

import javax.sql.DataSource;

import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnection;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.junit.After;
import org.junit.Before;

public abstract class AbstractSqlTest {

	private static final String H2_DRIVER = "org.h2.Driver";
	private static final String H2_URL = "jdbc:h2:mem:cmsmf";

	protected static final ConnectionFactory CONNECTION_FACTORY;

	static {
		if (!DbUtils.loadDriver(AbstractSqlTest.H2_DRIVER)) { throw new RuntimeException(String.format(
			"Failed to locate the JDBC driver class [%s]", AbstractSqlTest.H2_DRIVER)); }
		CONNECTION_FACTORY = new DriverManagerConnectionFactory(AbstractSqlTest.H2_URL, null);
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
		System.out.printf("Memory-based connection pool ready");
	}

	@After
	public void tearDown() throws Throwable {
		baseTearDown();
	}

	protected final void baseTearDown() {
		try {
			this.pool.close();
		} catch (Exception e) {
		}
		this.pool = null;
		this.dataSource = null;
		System.out.printf("Closing the memory-based connection pool");
	}

	protected final DataSource getDataSource() {
		return this.dataSource;
	}
}