package com.delta.cmsmf.cms.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.dbutils.QueryRunner;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.delta.cmsmf.cms.AbstractTest;
import com.delta.cmsmf.cms.CmsObjectType;
import com.delta.cmsmf.utils.DfUtils;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;

public class CmsObjectStoreTest extends AbstractTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void testConstructor() throws Throwable {
		CmsObjectStore store = null;
		QueryRunner qr = new QueryRunner(getDataSource());
		store = new CmsObjectStore(getDataSource(), true);
		// Make sure no data is there
		Assert.assertFalse(qr.query("select * from dctm_mapper", AbstractTest.HANDLER_EXISTS));
		Assert.assertFalse(qr.query("select * from dctm_object", AbstractTest.HANDLER_EXISTS));

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
			qr.query("select count(*) from dctm_mapper", AbstractTest.HANDLER_COUNT));
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

		store = new CmsObjectStore(getDataSource(), false);
		// Make sure the data is there
		Assert.assertEquals(Integer.valueOf(count),
			qr.query("select count(*) from dctm_mapper", AbstractTest.HANDLER_COUNT));
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

		store = new CmsObjectStore(getDataSource(), true);
		// Make sure all the data is gone
		Assert.assertEquals(Integer.valueOf(0),
			qr.query("select count(*) from dctm_mapper", AbstractTest.HANDLER_COUNT));
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
		QueryRunner qr = new QueryRunner(getDataSource());
		CmsObjectStore store = new CmsObjectStore(getDataSource(), true);
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
			qr.query("select count(*) from dctm_mapper", AbstractTest.HANDLER_COUNT));
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
		CmsObjectStore store = new CmsObjectStore(getDataSource(), true);
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
		CmsObjectStore store = new CmsObjectStore(getDataSource(), true);
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
		CmsObjectStore store = new CmsObjectStore(getDataSource(), true);
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
		CmsObjectStore store = new CmsObjectStore(getDataSource(), true);
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
		// QueryRunner qr = new QueryRunner(getDataSource());
		IDfSession session = acquireSession();
		try {

		} finally {
			releaseSession(session);
		}
	}

	@Test
	public void testDeserializeObjects() throws Throwable {
		// Assert.fail("Not yet implemented");
	}

	@Test
	public void testRegisterDependency() throws Throwable {
		final IDfSession session = acquireSession();
		CmsObjectStore store = new CmsObjectStore(getDataSource(), true);
		Map<String, CmsObjectType> dependencies = new HashMap<String, CmsObjectType>();
		try {
			String dql = "select r_object_id, r_object_type from dm_sysobject where folder('/CMSMFTests', DESCEND)";
			IDfCollection results = DfUtils.executeQuery(session, dql, IDfQuery.DF_EXECREAD_QUERY);
			try {
				while (results.next()) {
					String id = results.getString("r_object_id");
					CmsObjectType type = CmsObjectType.decodeType(results.getString("r_object_type"));
					store.persistDependency(type, id);
					dependencies.put(id, type);
				}
			} finally {
				closeQuietly(results);
			}
			QueryRunner qr = new QueryRunner(getDataSource());
			Assert.assertEquals(Integer.valueOf(dependencies.size()),
				qr.query("select count(*) from dctm_export_plan where traversed = false", AbstractTest.HANDLER_COUNT));
			for (Map.Entry<String, CmsObjectType> e : dependencies.entrySet()) {
				final String id = e.getKey();
				final CmsObjectType type = e.getValue();
				Assert
				.assertEquals(
					Integer.valueOf(1),
					qr.query(
						"select count(*) from dctm_export_plan where object_type = ? and object_id = ? and traversed = false",
						AbstractTest.HANDLER_COUNT, type.name(), id));
			}
		} finally {
			releaseSession(session);
		}
	}
}