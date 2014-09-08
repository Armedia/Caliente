package com.delta.cmsmf.cms.storage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

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
		System.out.printf("Preparing the memory-based connection pool");
		this.pool = new GenericObjectPool<PoolableConnection>();
		@SuppressWarnings("unused")
		final PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(
			CmsObjectStoreTest.CONNECTION_FACTORY, this.pool, null, null, false, true);
		this.dataSource = new PoolingDataSource(this.pool);
		System.out.printf("Memory-based connection pool ready");
	}

	@After
	public void tearDown() throws Throwable {
		this.pool.close();
		System.out.printf("Closing the memory-based connection pool");
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
	public void testSetMapping() throws Throwable {
		QueryRunner qr = new QueryRunner(this.dataSource);
		CmsObjectStore store = new CmsObjectStore(this.dataSource, true);
		try {
			store.setMapping(null, "something", "source", "target");
			Assert.fail("Did not fail with a null object type");
		} catch (IllegalArgumentException e) {
			// All is well
		}
		try {
			store.setMapping(CmsObjectType.ACL, null, "source", "target");
			Assert.fail("Did not fail with a null mapping name");
		} catch (IllegalArgumentException e) {
			// All is well
		}
		try {
			store.setMapping(CmsObjectType.ACL, "something", null, null);
			Assert.fail("Did not fail with both mapped values nulled");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		// add some mappings
		int count = 0;
		for (final CmsObjectType type : CmsObjectType.values()) {
			for (int a = 0; a < 10; a++) {
				final String mapping = String.format("mapping-%02d", a);
				for (int s = 0; s < 10; s++) {
					final String source = String.format("%02d", s);
					final String target = String.format("%02x", s | 0x00FF0000);
					store.setMapping(type, mapping, source, target);
					try {
						store.setMapping(type, mapping, source, target);
						Assert.fail(String.format(
							"Did not raise an exception while storing duplicate mapping [%s/%s/%s->%s]", type, mapping,
							source, target));
					} catch (Throwable e) {
						// All is well
					}
					try {
						String altTarget = UUID.randomUUID().toString();
						store.setMapping(type, mapping, source, altTarget);
						Assert.fail(String.format(
							"Did not raise an exception while storing duplicate mapping [%s/%s/%s->%s]", type, mapping,
							source, altTarget));
					} catch (Throwable e) {
						// All is well
					}
					try {
						String altSource = UUID.randomUUID().toString();
						store.setMapping(type, mapping, altSource, target);
						Assert.fail(String.format(
							"Did not raise an exception while storing duplicate mapping [%s/%s/%s->%s]", type, mapping,
							altSource, target));
					} catch (Throwable e) {
						// All is well
					}
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
	}

	@Test
	public void testClearSourceMapping() throws Throwable {
		CmsObjectStore store = new CmsObjectStore(this.dataSource, true);
		// add some mappings
		for (final CmsObjectType type : CmsObjectType.values()) {
			for (int a = 0; a < 10; a++) {
				final String mapping = String.format("mapping-%02d", a);
				for (int s = 0; s < 10; s++) {
					final String source = String.format("%02d", s);
					final String target = String.format("%02x", s | 0x00FF0000);
					store.setMapping(type, mapping, source, target);
					store.clearSourceMapping(type, mapping, target);
					Assert.assertNull(store.getSourceMapping(type, mapping, target));
				}
			}
		}
	}

	@Test
	public void testClearTargetMapping() throws Throwable {
		CmsObjectStore store = new CmsObjectStore(this.dataSource, true);
		// add some mappings
		for (final CmsObjectType type : CmsObjectType.values()) {
			for (int a = 0; a < 10; a++) {
				final String mapping = String.format("mapping-%02d", a);
				for (int s = 0; s < 10; s++) {
					final String source = String.format("%02d", s);
					final String target = String.format("%02x", s | 0x00FF0000);
					store.setMapping(type, mapping, source, target);
					store.clearTargetMapping(type, mapping, source);
					Assert.assertNull(store.getTargetMapping(type, mapping, source));
				}
			}
		}
	}

	@Test
	public void testGetTargetMapping() throws Throwable {
		CmsObjectStore store = new CmsObjectStore(this.dataSource, true);
		// add some mappings
		for (final CmsObjectType type : CmsObjectType.values()) {
			for (int a = 0; a < 10; a++) {
				final String mapping = String.format("mapping-%02d", a);
				for (int s = 0; s < 10; s++) {
					final String source = String.format("%02d", s);
					final String target = String.format("%02x", s | 0x00FF0000);
					store.setMapping(type, mapping, source, target);
					Assert.assertEquals(target, store.getTargetMapping(type, mapping, source));
				}
			}
		}
	}

	@Test
	public void testGetSourceMapping() throws Throwable {
		CmsObjectStore store = new CmsObjectStore(this.dataSource, true);
		// add some mappings
		for (final CmsObjectType type : CmsObjectType.values()) {
			for (int a = 0; a < 10; a++) {
				final String mapping = String.format("mapping-%02d", a);
				for (int s = 0; s < 10; s++) {
					final String source = String.format("%02d", s);
					final String target = String.format("%02x", s | 0x00FF0000);
					store.setMapping(type, mapping, source, target);
					Assert.assertEquals(source, store.getSourceMapping(type, mapping, target));
				}
			}
		}
	}

	@Test
	public void testSerializeObject() throws Throwable {
		// Assert.fail("Not yet implemented");
	}

	@Test
	public void testDeserializeObjects() throws Throwable {
		// Assert.fail("Not yet implemented");
	}
}