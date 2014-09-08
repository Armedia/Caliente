package com.delta.cmsmf.cms.storage;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnection;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.delta.cmsmf.cms.CmsObjectType;

public class CmsObjectStoreTest {

	private static final String H2_DRIVER = "org.h2.Driver";
	private static final String H2_URL = "jdbc:h2:mem:cmsmf";

	private static final ConnectionFactory CONNECTION_FACTORY;

	static {
		if (!DbUtils.loadDriver(CmsObjectStoreTest.H2_DRIVER)) { throw new RuntimeException(String.format(
			"Failed to locate the JDBC driver class [%s]", CmsObjectStoreTest.H2_DRIVER)); }
		CONNECTION_FACTORY = new DriverManagerConnectionFactory(CmsObjectStoreTest.H2_URL, null);
	}

	private DataSource dataSource = null;
	private ObjectPool<PoolableConnection> pool = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Throwable {
		this.pool = new GenericObjectPool<PoolableConnection>();
		@SuppressWarnings("unused")
		final PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(
			CmsObjectStoreTest.CONNECTION_FACTORY, this.pool, null, null, false, true);
		this.dataSource = new PoolingDataSource(this.pool);
	}

	@After
	public void tearDown() throws Throwable {
		this.pool.close();
	}

	@Test
	public void testConstructor() throws Throwable {
		CmsObjectStore store = null;
		QueryRunner qr = new QueryRunner(this.dataSource);
		store = new CmsObjectStore(this.dataSource, true);
		// Make sure no data is there
		Assert.assertFalse(qr.query("select * from dctm_mapper", new ResultSetHandler<Boolean>() {
			@Override
			public Boolean handle(ResultSet rs) throws SQLException {
				return rs.next();
			}
		}));
		Assert.assertFalse(qr.query("select * from dctm_object", new ResultSetHandler<Boolean>() {
			@Override
			public Boolean handle(ResultSet rs) throws SQLException {
				return rs.next();
			}
		}));

		// add some data
		int count = 0;
		for (final CmsObjectType type : CmsObjectType.values()) {
			for (int a = 0; a < 10; a++) {
				final String mapping = String.format("mapping-%02d", a);
				for (int s = 0; s < 10; s++) {
					final String source = String.format("%02d", s);
					final String target = String.format("%02x", s | 0x00FF0000);
					store.setMapping(type, mapping, source, target);
					count++;
				}
			}
		}

		// Make sure it's there
		Assert.assertEquals(Integer.valueOf(count),
			qr.query("select count(*) from dctm_mapper", new ResultSetHandler<Integer>() {
				@Override
				public Integer handle(ResultSet rs) throws SQLException {
					if (rs.next()) { return rs.getInt(1); }
					return 0;
				}
			}));
		// More detailed check
		for (final CmsObjectType type : CmsObjectType.values()) {
			for (int a = 0; a < 10; a++) {
				final String mapping = String.format("mapping-%02d", a);
				for (int s = 0; s < 10; s++) {
					final String source = String.format("%02d", s);
					final String target = String.format("%02x", s | 0x00FF0000);
					String actualSource = store.getSourceMapping(type, mapping, target);
					Assert.assertEquals(source, actualSource);
					String actualTarget = store.getTargetMapping(type, mapping, source);
					Assert.assertEquals(target, actualTarget);
				}
			}
		}

		store = new CmsObjectStore(this.dataSource, false);
		// Make sure the data is there
		Assert.assertEquals(Integer.valueOf(count),
			qr.query("select count(*) from dctm_mapper", new ResultSetHandler<Integer>() {
				@Override
				public Integer handle(ResultSet rs) throws SQLException {
					if (rs.next()) { return rs.getInt(1); }
					return 0;
				}
			}));
		// More detailed check
		for (final CmsObjectType type : CmsObjectType.values()) {
			for (int a = 0; a < 10; a++) {
				final String mapping = String.format("mapping-%02d", a);
				for (int s = 0; s < 10; s++) {
					final String source = String.format("%02d", s);
					final String target = String.format("%02x", s | 0x00FF0000);
					String actualSource = store.getSourceMapping(type, mapping, target);
					Assert.assertEquals(source, actualSource);
					String actualTarget = store.getTargetMapping(type, mapping, source);
					Assert.assertEquals(target, actualTarget);
				}
			}
		}

		store = new CmsObjectStore(this.dataSource, true);
		// Make sure all the data is gone
		Assert.assertEquals(Integer.valueOf(0),
			qr.query("select count(*) from dctm_mapper", new ResultSetHandler<Integer>() {
				@Override
				public Integer handle(ResultSet rs) throws SQLException {
					if (rs.next()) { return rs.getInt(1); }
					return 0;
				}
			}));
		// More detailed check
		for (final CmsObjectType type : CmsObjectType.values()) {
			for (int a = 0; a < 10; a++) {
				final String mapping = String.format("mapping-%02d", a);
				for (int s = 0; s < 10; s++) {
					final String source = String.format("%02d", s);
					final String target = String.format("%02x", s | 0x00FF0000);
					Assert.assertNull(store.getSourceMapping(type, mapping, target));
					Assert.assertNull(store.getTargetMapping(type, mapping, source));
				}
			}
		}
	}

	@Test
	public void testSetMapping() {
		Assert.fail("Not yet implemented");
	}

	@Test
	public void testClearSourceMapping() {
		Assert.fail("Not yet implemented");
	}

	@Test
	public void testClearTargetMapping() {
		Assert.fail("Not yet implemented");
	}

	@Test
	public void testGetTargetMapping() {
		Assert.fail("Not yet implemented");
	}

	@Test
	public void testGetSourceMapping() {
		Assert.fail("Not yet implemented");
	}

	@Test
	public void testSerializeObject() {
		Assert.fail("Not yet implemented");
	}

	@Test
	public void testDeserializeObjects() {
		Assert.fail("Not yet implemented");
	}
}