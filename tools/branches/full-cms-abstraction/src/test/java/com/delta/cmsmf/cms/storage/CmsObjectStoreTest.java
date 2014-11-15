package com.delta.cmsmf.cms.storage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.delta.cmsmf.cms.AbstractTest;
import com.delta.cmsmf.cms.DctmACL;
import com.delta.cmsmf.cms.CmsAttribute;
import com.delta.cmsmf.cms.CmsAttributeMapper;
import com.delta.cmsmf.cms.CmsAttributeMapper.Mapping;
import com.delta.cmsmf.cms.DctmDataType;
import com.delta.cmsmf.cms.CmsFileSystem;
import com.delta.cmsmf.cms.DctmMappingUtils;
import com.delta.cmsmf.cms.CmsBaseObject;
import com.delta.cmsmf.cms.CmsBaseObjectType;
import com.delta.cmsmf.cms.CmsProperty;
import com.delta.cmsmf.cms.DctmTransferContext;
import com.delta.cmsmf.cms.DctmContentStreamStore;
import com.delta.cmsmf.cms.DefaultTransferContext;
import com.delta.cmsmf.cms.DfValueFactory;
import com.delta.cmsmf.cms.UnsupportedObjectTypeException;
import com.delta.cmsmf.cms.storage.CmsBaseObjectStore.ObjectHandler;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.utils.DfUtils;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfValue;

public class CmsBaseObjectStoreTest extends AbstractTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void testConstructor() throws Throwable {
		CmsBaseObjectStore store = null;
		QueryRunner qr = new QueryRunner(getDataSource());
		store = new CmsBaseObjectStore(getDataSource(), true);
		// Make sure no data is there
		Assert.assertFalse(qr.query("select * from dctm_mapper", AbstractTest.HANDLER_EXISTS));
		Assert.assertFalse(qr.query("select * from dctm_object", AbstractTest.HANDLER_EXISTS));

		StoredAttributeMapper mapper = store.getAttributeMapper();

		// add some data
		int count = 0;
		for (final CmsBaseObjectType type : CmsBaseObjectType.values()) {
			for (int a = 0; a < 10; a++) {
				final String mapping = String.format("mapping-%02d", a);
				for (int s = 0; s < 10; s++) {
					final String source = String.format("%02d", s);
					final String target = String.format("%02x", s | 0x00FF0000);
					mapper.setMapping(type, mapping, source, target);
					count++;
				}
			}
		}

		// Make sure it's there
		Assert.assertEquals(Integer.valueOf(count),
			qr.query("select count(*) from dctm_mapper", AbstractTest.HANDLER_COUNT));
		// More detailed check
		for (final CmsBaseObjectType type : CmsBaseObjectType.values()) {
			for (int a = 0; a < 10; a++) {
				final String mapping = String.format("mapping-%02d", a);
				for (int s = 0; s < 10; s++) {
					final String source = String.format("%02d", s);
					final String target = String.format("%02x", s | 0x00FF0000);
					Mapping actualSource = mapper.getSourceMapping(type, mapping, target);
					Assert.assertEquals(source, actualSource.getSourceValue());
					Mapping actualTarget = mapper.getTargetMapping(type, mapping, source);
					Assert.assertEquals(target, actualTarget.getTargetValue());
				}
			}
		}

		store = new CmsBaseObjectStore(getDataSource(), false);
		// Make sure the data is there
		Assert.assertEquals(Integer.valueOf(count),
			qr.query("select count(*) from dctm_mapper", AbstractTest.HANDLER_COUNT));
		// More detailed check
		for (final CmsBaseObjectType type : CmsBaseObjectType.values()) {
			for (int a = 0; a < 10; a++) {
				final String mapping = String.format("mapping-%02d", a);
				for (int s = 0; s < 10; s++) {
					final String source = String.format("%02d", s);
					final String target = String.format("%02x", s | 0x00FF0000);
					Mapping actualSource = mapper.getSourceMapping(type, mapping, target);
					Assert.assertEquals(source, actualSource.getSourceValue());
					Mapping actualTarget = mapper.getTargetMapping(type, mapping, source);
					Assert.assertEquals(target, actualTarget.getTargetValue());
				}
			}
		}

		store = new CmsBaseObjectStore(getDataSource(), true);
		// Make sure all the data is gone
		Assert.assertEquals(Integer.valueOf(0),
			qr.query("select count(*) from dctm_mapper", AbstractTest.HANDLER_COUNT));
		// More detailed check
		for (final CmsBaseObjectType type : CmsBaseObjectType.values()) {
			for (int a = 0; a < 10; a++) {
				final String mapping = String.format("mapping-%02d", a);
				for (int s = 0; s < 10; s++) {
					final String source = String.format("%02d", s);
					final String target = String.format("%02x", s | 0x00FF0000);
					Assert.assertNull(mapper.getSourceMapping(type, mapping, target));
					Assert.assertNull(mapper.getTargetMapping(type, mapping, source));
				}
			}
		}
	}

	@Test
	public void testSetMapping() throws Throwable {
		QueryRunner qr = new QueryRunner(getDataSource());
		CmsBaseObjectStore store = new CmsBaseObjectStore(getDataSource(), true);
		StoredAttributeMapper mapper = store.getAttributeMapper();
		try {
			mapper.setMapping(null, "something", "source", "target");
			Assert.fail("Did not fail with a null object type");
		} catch (IllegalArgumentException e) {
			// All is well
		}
		try {
			mapper.setMapping(CmsBaseObjectType.ACL, null, "source", "target");
			Assert.fail("Did not fail with a null mapping name");
		} catch (IllegalArgumentException e) {
			// All is well
		}
		try {
			mapper.setMapping(CmsBaseObjectType.ACL, "something", null, null);
			Assert.fail("Did not fail with both mapped values nulled");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		// add some mappings
		int count = 0;
		for (final CmsBaseObjectType type : CmsBaseObjectType.values()) {
			for (int a = 0; a < 10; a++) {
				final String mapping = String.format("mapping-%02d", a);
				for (int s = 0; s < 10; s++) {
					final String source = String.format("%02d", s);
					final String target = String.format("%02x", s | 0x00FF0000);
					mapper.setMapping(type, mapping, source, target);
					try {
						mapper.setMapping(type, mapping, source, target);
						Assert.fail(String.format(
							"Did not raise an exception while storing duplicate mapping [%s/%s/%s->%s]", type, mapping,
							source, target));
					} catch (Throwable e) {
						// All is well
					}
					try {
						String altTarget = UUID.randomUUID().toString();
						mapper.setMapping(type, mapping, source, altTarget);
						Assert.fail(String.format(
							"Did not raise an exception while storing duplicate mapping [%s/%s/%s->%s]", type, mapping,
							source, altTarget));
					} catch (Throwable e) {
						// All is well
					}
					try {
						String altSource = UUID.randomUUID().toString();
						mapper.setMapping(type, mapping, altSource, target);
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
		for (final CmsBaseObjectType type : CmsBaseObjectType.values()) {
			for (int a = 0; a < 10; a++) {
				final String mapping = String.format("mapping-%02d", a);
				for (int s = 0; s < 10; s++) {
					final String source = String.format("%02d", s);
					final String target = String.format("%02x", s | 0x00FF0000);
					Mapping actualSource = mapper.getSourceMapping(type, mapping, target);
					Assert.assertEquals(source, actualSource.getSourceValue());
					Mapping actualTarget = mapper.getTargetMapping(type, mapping, source);
					Assert.assertEquals(target, actualTarget.getTargetValue());
				}
			}
		}
	}

	@Test
	public void testClearSourceMapping() throws Throwable {
		CmsBaseObjectStore store = new CmsBaseObjectStore(getDataSource(), true);
		StoredAttributeMapper mapper = store.getAttributeMapper();
		// add some mappings
		for (final CmsBaseObjectType type : CmsBaseObjectType.values()) {
			for (int a = 0; a < 10; a++) {
				final String mapping = String.format("mapping-%02d", a);
				for (int s = 0; s < 10; s++) {
					final String source = String.format("%02d", s);
					final String target = String.format("%02x", s | 0x00FF0000);
					mapper.setMapping(type, mapping, source, target);
					mapper.clearSourceMapping(type, mapping, target);
					Assert.assertNull(mapper.getSourceMapping(type, mapping, target));
				}
			}
		}
	}

	@Test
	public void testClearTargetMapping() throws Throwable {
		CmsBaseObjectStore store = new CmsBaseObjectStore(getDataSource(), true);
		StoredAttributeMapper mapper = store.getAttributeMapper();
		// add some mappings
		for (final CmsBaseObjectType type : CmsBaseObjectType.values()) {
			for (int a = 0; a < 10; a++) {
				final String mapping = String.format("mapping-%02d", a);
				for (int s = 0; s < 10; s++) {
					final String source = String.format("%02d", s);
					final String target = String.format("%02x", s | 0x00FF0000);
					mapper.setMapping(type, mapping, source, target);
					mapper.clearTargetMapping(type, mapping, source);
					Assert.assertNull(mapper.getTargetMapping(type, mapping, source));
				}
			}
		}
	}

	@Test
	public void testGetTargetMapping() throws Throwable {
		CmsBaseObjectStore store = new CmsBaseObjectStore(getDataSource(), true);
		StoredAttributeMapper mapper = store.getAttributeMapper();
		// add some mappings
		for (final CmsBaseObjectType type : CmsBaseObjectType.values()) {
			for (int a = 0; a < 10; a++) {
				final String mapping = String.format("mapping-%02d", a);
				for (int s = 0; s < 10; s++) {
					final String source = String.format("%02d", s);
					final String target = String.format("%02x", s | 0x00FF0000);
					Mapping expected = mapper.setMapping(type, mapping, source, target);
					Mapping actual = mapper.getTargetMapping(type, mapping, source);
					Assert.assertEquals(expected, actual);
				}
			}
		}
	}

	@Test
	public void testGetSourceMapping() throws Throwable {
		CmsBaseObjectStore store = new CmsBaseObjectStore(getDataSource(), true);
		StoredAttributeMapper mapper = store.getAttributeMapper();
		// add some mappings
		for (final CmsBaseObjectType type : CmsBaseObjectType.values()) {
			for (int a = 0; a < 10; a++) {
				final String mapping = String.format("mapping-%02d", a);
				for (int s = 0; s < 10; s++) {
					final String source = String.format("%02d", s);
					final String target = String.format("%02x", s | 0x00FF0000);
					Mapping expected = mapper.setMapping(type, mapping, source, target);
					Mapping actual = mapper.getSourceMapping(type, mapping, target);
					Assert.assertEquals(expected, actual);
				}
			}
		}
	}

	@Test
	public void testSerializeObject() throws Throwable {
		final CmsFileSystem fs = new DctmContentStreamStore(getFsDir());
		CmsBaseObjectStore store = new CmsBaseObjectStore(getDataSource(), true);
		final QueryRunner qr = new QueryRunner(getDataSource());
		DctmTransferContext ctx = new DefaultTransferContext(null, null, store, fs, null);
		try {
			store.serializeObject(null, ctx);
			Assert.fail("Did not fail with a null object");
		} catch (IllegalArgumentException e) {
			// All is well
		}
		try {
			store.serializeObject(null, null);
			Assert.fail("Did not fail with a null object and context");
		} catch (IllegalArgumentException e) {
			// All is well
		}
		try {
			store.serializeObject(CmsBaseObjectType.TYPE.newInstance(), null);
			Assert.fail("Did not fail with a null context");
		} catch (IllegalArgumentException e) {
			// All is well
		}
		IDfSession session = acquireSourceSession();
		try {
			final int max = 3;
			for (CmsBaseObjectType t : CmsBaseObjectType.values()) {
				final CmsBaseObject<? extends IDfPersistentObject> obj = t.newInstance();
				final DocumentumType dt = DocumentumType.decode(obj);
				IDfCollection results = DfUtils.executeQuery(session,
					String.format("select r_object_id from %s", dt.dmTable, max, max), IDfQuery.DF_EXECREAD_QUERY);
				try {
					int count = 0;
					while (results.next()) {
						IDfId id = results.getId("r_object_id");
						final IDfPersistentObject cmsObj = session.getObject(id);
						try {
							CmsBaseObjectType.decodeType(cmsObj);
						} catch (IllegalArgumentException e) {
							if (this.log.isDebugEnabled()) {
								this.log.debug(String.format(
									"Found an object of type [%s] while scanning for objects of type [%s]", cmsObj
										.getType().getName(), t));
							}
							continue;
						} catch (UnsupportedDctmObjectTypeException e) {
							this.log.info(e.getMessage());
							continue;
						}
						if (!obj.loadFromCMS(cmsObj)) {
							// Unsupported object
							continue;
						}
						Assert.assertEquals(id.getId(), obj.getId());
						Assert.assertEquals(Integer.valueOf(0), qr.query(
							"select count(*) from dctm_object where object_id = ?", AbstractTest.HANDLER_COUNT,
							id.getId()));
						ctx = new DefaultTransferContext(obj.getId(), session, store, fs, null);
						Assert.assertTrue(store.serializeObject(obj, ctx));
						Assert.assertEquals(Integer.valueOf(1), qr.query(
							"select count(*) from dctm_object where object_id = ?", AbstractTest.HANDLER_COUNT,
							id.getId()));
						Assert.assertFalse(store.serializeObject(obj, ctx));
						Assert.assertEquals(Integer.valueOf(obj.getAttributeCount()), qr.query(
							"select count(*) from dctm_attribute where object_id = ?", AbstractTest.HANDLER_COUNT,
							id.getId()));
						Assert.assertEquals(Integer.valueOf(obj.getPropertyCount()), qr.query(
							"select count(*) from dctm_property where object_id = ?", AbstractTest.HANDLER_COUNT,
							id.getId()));
						qr.query("select * from dctm_attribute where object_id = ?", new ResultSetHandler<Void>() {
							@Override
							public Void handle(ResultSet rs) throws SQLException {
								int a = 0;
								while (rs.next()) {
									a++;
									final String objectId = rs.getString("object_id");
									final String name = rs.getString("name");
									final DctmDataType dataType = DctmDataType.valueOf(rs.getString("data_type"));
									final String id = rs.getString("id");
									final int length = rs.getInt("length");
									final boolean qualifiable = rs.getBoolean("qualifiable");
									final boolean repeating = rs.getBoolean("repeating");
									final IDfAttr attr;
									try {
										attr = cmsObj.getAttr(cmsObj.findAttrIndex(name));
									} catch (DfException e) {
										String msg = String.format(
											"Failed to retrieve attribute [%s] for object [%s:%s]", name,
											obj.getType(), obj.getId());
										CmsBaseObjectStoreTest.this.log.fatal(msg, e);
										Assert.fail(msg);
										return null;
									}
									Assert.assertNotNull(attr);
									Assert.assertEquals(obj.getId(), objectId);
									Assert.assertEquals(DctmDataType.fromAttribute(attr), dataType);
									Assert.assertEquals(attr.getName(), name);
									Assert.assertEquals(attr.getId(), id);
									Assert.assertEquals(attr.getLength(), length);
									Assert.assertEquals(attr.isQualifiable(), qualifiable);
									Assert.assertEquals(attr.isRepeating(), repeating);
									qr.query("select * from dctm_attribute_value where object_id = ? and name = ?",
										new ResultSetHandler<Void>() {
											@Override
											public Void handle(ResultSet rs) throws SQLException {
												int num = 0;
												while (rs.next()) {
													final String objectId = rs.getString("object_id");
													final String name = rs.getString("name");
													final int valueNum = rs.getInt("value_number");
													final String data = rs.getString("data");
													Assert.assertEquals(obj.getId(), objectId);
													Assert.assertEquals(attr.getName(), name);
													Assert.assertEquals(num, valueNum);
													final IDfValue expected;
													try {
														expected = cmsObj.getRepeatingValue(name, valueNum);
													} catch (DfException e) {
														Assert.fail(String
															.format(
																"Failed to get repeating value #%d for attribute %s for object [%s:%s]",
																valueNum, name, obj.getType(), obj.getId()));
														return null;
													}
													IDfValue decoded = dataType.decode(data);
													if (dataType == DctmDataType.DF_STRING) {
														try {
															decoded = DfValueFactory.newStringValue(DctmMappingUtils
																.resolveMappableUser(cmsObj, decoded.asString()));
														} catch (DfException e) {
															Assert.fail(String
																.format("Failed to resolve the special user attribute"));
														}
													}
													Assert.assertEquals(
														String.format("Expectation failed on attribute [%s.%s]",
															obj.getSubtype(), name), dataType.getValue(expected),
														dataType.getValue(decoded));
													num++;
												}
												try {
													Assert.assertEquals(cmsObj.getValueCount(name), num);
												} catch (DfException e) {
													Assert.fail(String
														.format(
															"Failed to get value count for attribute %s for object [%s:%s]",
															name, obj.getType(), obj.getId()));
												}
												return null;
											}
										}, obj.getId(), name);
								}
								Assert.assertEquals(
									String.format("Failed to validate the attributes for object [%s:%s]",
										obj.getType(), obj.getId()), obj.getAttributeCount(), a);
								return null;
							}
						}, id.getId());
						qr.query("select * from dctm_property where object_id = ?", new ResultSetHandler<Void>() {
							@Override
							public Void handle(ResultSet rs) throws SQLException {
								int p = 0;
								while (rs.next()) {
									p++;
									final String objectId = rs.getString("object_id");
									final String name = rs.getString("name");
									final DctmDataType dataType = DctmDataType.valueOf(rs.getString("data_type"));
									final boolean repeating = rs.getBoolean("repeating");
									final StoredProperty property = obj.getProperty(name);
									Assert.assertNotNull(property);
									Assert.assertEquals(obj.getId(), objectId);
									Assert.assertEquals(property.getType(), dataType);
									Assert.assertEquals(property.getName(), name);
									Assert.assertEquals(property.isRepeating(), repeating);
									qr.query("select * from dctm_property_value where object_id = ? and name = ?",
										new ResultSetHandler<Void>() {
											@Override
											public Void handle(ResultSet rs) throws SQLException {
												int num = 0;
												while (rs.next()) {
													final String objectId = rs.getString("object_id");
													final String name = rs.getString("name");
													final int valueNum = rs.getInt("value_number");
													final String data = rs.getString("data");
													Assert.assertEquals(obj.getId(), objectId);
													Assert.assertEquals(property.getName(), name);
													Assert.assertEquals(num, valueNum);
													IDfValue expected = property.getValue(valueNum);
													IDfValue decoded = dataType.decode(data);
													Assert.assertEquals(dataType.getValue(expected),
														dataType.getValue(decoded));
													num++;
												}
												Assert.assertEquals(property.getValueCount(), num);
												return null;
											}
										}, obj.getId(), name);
								}
								Assert.assertEquals(
									String.format("Failed to validate the properties for object [%s:%s]",
										obj.getType(), obj.getId()), obj.getPropertyCount(), p);
								return null;
							}
						}, id.getId());
						if (++count > max) {
							break;
						}
					}
					if (count == 0) {
						Assert.fail(String.format("Did not find any objects of type [%s] to test against", t));
					}
				} finally {
					closeQuietly(results);
				}
			}
		} finally {
			releaseSourceSession(session);
		}
	}

	@Test
	public void testDeserializeObjects() throws Throwable {
		final CmsFileSystem fs = new DctmContentStreamStore(getFsDir());
		CmsBaseObjectStore store = new CmsBaseObjectStore(getDataSource(), true);
		try {
			store.deserializeObjects(null, null);
			Assert.fail("Did not fail with both parameters null");
		} catch (IllegalArgumentException e) {
			// All is well
		}
		try {
			store.deserializeObjects(DctmACL.class, null);
			Assert.fail("Did not fail with id null");
		} catch (IllegalArgumentException e) {
			// All is well
		}
		try {
			store.deserializeObjects(null, new StoredObjectHandler() {

				@Override
				public boolean newBatch(String batchId) throws CMSMFException {
					return true;
				}

				@Override
				public void handle(CmsBaseObject<?> dataObject) throws CMSMFException {
				}

				@Override
				public boolean closeBatch(boolean ok) throws CMSMFException {
					return true;
				}
			});
			Assert.fail("Did not fail with type null");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		IDfSession session = acquireSourceSession();
		try {
			final int max = 3;
			for (final CmsBaseObjectType t : CmsBaseObjectType.values()) {
				final Map<String, CmsBaseObject<?>> expected = new HashMap<String, CmsBaseObject<?>>();
				final DocumentumType dt = DocumentumType.decode(t);
				IDfCollection results = DfUtils.executeQuery(session,
					String.format("select r_object_id from %s", dt.dmTable, max, max), IDfQuery.DF_EXECREAD_QUERY);
				try {
					int count = 0;
					while (results.next()) {
						IDfId id = results.getId("r_object_id");
						final IDfPersistentObject cmsObj = session.getObject(id);
						try {
							CmsBaseObjectType.decodeType(cmsObj);
						} catch (IllegalArgumentException e) {
							if (this.log.isDebugEnabled()) {
								this.log.debug(String.format(
									"Found an object of type [%s] while scanning for objects of type [%s]", cmsObj
										.getType().getName(), t));
							}
							continue;
						} catch (UnsupportedDctmObjectTypeException e) {
							this.log.info(e.getMessage());
							continue;
						}
						final CmsBaseObject<? extends IDfPersistentObject> obj = t.newInstance();
						if (!obj.loadFromCMS(cmsObj)) {
							// Unsupported object
							continue;
						}
						DctmTransferContext ctx = new DefaultTransferContext(obj.getId(), session, store, fs, null);
						store.serializeObject(obj, ctx);
						expected.put(obj.getId(), obj);
						if (++count > max) {
							break;
						}
					}
					if (count == 0) {
						Assert.fail(String.format("Did not find any objects of type [%s] to test against", t));
					}
				} finally {
					closeQuietly(results);
				}

				// Now, try to deserialize
				store.deserializeObjects(t.getCmsBaseObjectClass(), new StoredObjectHandler() {
					private String currentBatch = null;

					@Override
					public boolean newBatch(String batchId) throws CMSMFException {
						Assert.assertNotNull(batchId);
						if (t.isBatchingSupported()) {
							this.currentBatch = batchId;
						} else {
							this.currentBatch = CmsBaseObject.NULL_BATCH_ID;
						}
						return true;
					}

					@Override
					public void handle(CmsBaseObject<?> obj) throws CMSMFException {
						Assert.assertEquals(this.currentBatch, obj.getBatchId());

						final CmsBaseObject<?> expectedObject = expected.get(obj.getId());
						Assert.assertNotNull(expectedObject);
						// Compare them both
						Assert.assertSame(expectedObject.getClass(), obj.getClass());
						Assert.assertEquals(expectedObject.getId(), obj.getId());
						Assert.assertSame(expectedObject.getType(), obj.getType());
						Assert.assertSame(expectedObject.getDfClass(), obj.getDfClass());

						// Attributes
						Assert.assertEquals(expectedObject.getAttributeCount(), obj.getAttributeCount());
						for (String attName : expectedObject.getAttributeNames()) {
							StoredAttribute attA = expectedObject.getAttribute(attName);
							StoredAttribute attB = obj.getAttribute(attName);
							Assert.assertNotNull(attB);
							Assert.assertTrue(attA.isSame(attB));
							Assert.assertTrue(attA.isSameValues(attB));
						}
						// Properties
						Assert.assertEquals(expectedObject.getPropertyCount(), obj.getPropertyCount());
						for (String propName : expectedObject.getPropertyNames()) {
							StoredProperty propA = expectedObject.getProperty(propName);
							StoredProperty propB = obj.getProperty(propName);
							Assert.assertNotNull(propB);
							Assert.assertTrue(propA.isSame(propB));
							Assert.assertTrue(propA.isSameValues(propB));
						}
					}

					@Override
					public boolean closeBatch(boolean ok) throws CMSMFException {
						return true;
					}
				});
			}
		} finally {
			releaseSourceSession(session);
		}
	}

	@Test
	public void testRegisterDependency() throws Throwable {
		final CmsFileSystem fs = new DctmContentStreamStore(getFsDir());
		final IDfSession session = acquireSourceSession();
		CmsBaseObjectStore store = new CmsBaseObjectStore(getDataSource(), true);
		Map<String, CmsBaseObjectType> dependencies = new HashMap<String, CmsBaseObjectType>();
		try {
			String dql = "select r_object_id, r_object_type from dm_sysobject where folder('/CMSMFTests', DESCEND)";
			IDfCollection results = DfUtils.executeQuery(session, dql, IDfQuery.DF_EXECREAD_QUERY);
			try {
				while (results.next()) {
					String id = results.getString("r_object_id");
					CmsBaseObjectType type = CmsBaseObjectType.decodeType(results.getString("r_object_type"));
					DctmTransferContext ctx = new DefaultTransferContext(id, session, store, fs, null);
					store.persistDependency(type, id, ctx);
					dependencies.put(id, type);
				}
			} finally {
				closeQuietly(results);
			}
			QueryRunner qr = new QueryRunner(getDataSource());
			Assert.assertEquals(Integer.valueOf(dependencies.size()),
				qr.query("select count(*) from dctm_export_plan where traversed = false", AbstractTest.HANDLER_COUNT));
			for (Map.Entry<String, CmsBaseObjectType> e : dependencies.entrySet()) {
				final String id = e.getKey();
				final CmsBaseObjectType type = e.getValue();
				Assert
					.assertEquals(
						Integer.valueOf(1),
						qr.query(
							"select count(*) from dctm_export_plan where object_type = ? and object_id = ? and traversed = false",
							AbstractTest.HANDLER_COUNT, type.name(), id));
			}
		} finally {
			releaseSourceSession(session);
		}
	}
}