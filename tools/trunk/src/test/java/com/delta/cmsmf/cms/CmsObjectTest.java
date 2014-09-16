/**
 *
 */

package com.delta.cmsmf.cms;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.junit.Assert;
import org.junit.Test;

import com.delta.cmsmf.cms.storage.CmsObjectStore;
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

/**
 * @author Diego Rivera <diego.rivera@armedia.com>
 *
 */
public class CmsObjectTest extends AbstractTest {

	/**
	 * Test method for {@link com.delta.cmsmf.cms.CmsObject#CmsObject(java.lang.Class)} .
	 *
	 * @throws CMSMFException
	 */
	@Test
	public void testCmsObjectConstruction() throws Throwable {
		IDfSession session = acquireSourceSession();
		try {
			final int max = 3;
			for (CmsObjectType t : CmsObjectType.values()) {
				if (t == CmsObjectType.DOCUMENT_REF) {
					continue;
				}
				CmsObject<? extends IDfPersistentObject> obj = t.newInstance();
				final DocumentumType dt = DocumentumType.decode(obj);
				IDfCollection results = DfUtils.executeQuery(session,
					String.format("select r_object_id from %s", dt.dmTable, max, max), IDfQuery.DF_EXECREAD_QUERY);
				try {
					int count = 0;
					while (results.next()) {
						IDfId id = results.getId("r_object_id");
						IDfPersistentObject cmsObj = session.getObject(id);
						try {
							CmsObjectType.decodeType(cmsObj);
						} catch (IllegalArgumentException e) {
							if (this.log.isDebugEnabled()) {
								this.log.debug(String.format(
									"Found an object of type [%s] while scanning for objects of type [%s]", cmsObj
									.getType().getName(), t));
							}
							continue;
						} catch (UnsupportedObjectTypeException e) {
							// Object type is not supported
							this.log.info(e.getMessage());
							continue;
						}
						if (!obj.loadFromCMS(cmsObj)) {
							// Unsupported object
							continue;
						}
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

	/**
	 * Test method for {@link com.delta.cmsmf.cms.CmsObject#load(java.sql.ResultSet)}.
	 */
	@Test
	public void testCmsObjectPersistence() throws Throwable {
		final CmsObjectStore store = new CmsObjectStore(getDataSource(), true);
		final QueryRunner qr = new QueryRunner(getDataSource());
		final CmsFileSystem fs = new DefaultCmsFileSystem(getFsDir());
		IDfSession session = acquireSourceSession();
		try {
			final int max = 3;
			for (CmsObjectType t : CmsObjectType.values()) {
				if (t == CmsObjectType.DOCUMENT_REF) {
					continue;
				}
				final CmsObject<? extends IDfPersistentObject> obj = t.newInstance();
				final DocumentumType dt = DocumentumType.decode(obj);
				IDfCollection results = DfUtils.executeQuery(session,
					String.format("select r_object_id from %s", dt.dmTable, max, max), IDfQuery.DF_EXECREAD_QUERY);
				try {
					int count = 0;
					while (results.next()) {
						IDfId id = results.getId("r_object_id");
						final IDfPersistentObject cmsObj = session.getObject(id);
						try {
							CmsObjectType.decodeType(cmsObj);
						} catch (IllegalArgumentException e) {
							if (this.log.isDebugEnabled()) {
								this.log.debug(String.format(
									"Found an object of type [%s] while scanning for objects of type [%s]", cmsObj
									.getType().getName(), t));
							}
							continue;
						} catch (UnsupportedObjectTypeException e) {
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
						CmsTransferContext ctx = new DefaultTransferContext(obj.getId(), session, store, fs);
						store.serializeObject(obj, ctx);
						Assert.assertEquals(Integer.valueOf(1), qr.query(
							"select count(*) from dctm_object where object_id = ?", AbstractTest.HANDLER_COUNT,
							id.getId()));
						Assert.assertEquals(Integer.valueOf(obj.getAttributeCount()), qr.query(
							"select count(*) from dctm_attribute where object_id = ?", AbstractTest.HANDLER_COUNT,
							id.getId()));
						Assert.assertEquals(Integer.valueOf(obj.getPropertyCount()), qr.query(
							"select count(*) from dctm_property where object_id = ?", AbstractTest.HANDLER_COUNT,
							id.getId()));
						qr.query("select * from dctm_attribute where object_id = ?", new ResultSetHandler<Void>() {
							@Override
							public Void handle(ResultSet rs) throws SQLException {
								boolean explode = true;
								while (rs.next()) {
									explode = false;
									final String objectId = rs.getString("object_id");
									final String name = rs.getString("name");
									final CmsDataType dataType = CmsDataType.valueOf(rs.getString("data_type"));
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
										CmsObjectTest.this.log.fatal(msg, e);
										Assert.fail(msg);
										return null;
									}
									Assert.assertNotNull(attr);
									Assert.assertEquals(obj.getId(), objectId);
									Assert.assertEquals(CmsDataType.fromAttribute(attr), dataType);
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
												if (dataType == CmsDataType.DF_STRING) {
													try {
														decoded = DfValueFactory.newStringValue(CmsMappingUtils
																.resolveSpecialUser(cmsObj, decoded.asString()));
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
								Assert.assertFalse(
									String.format("Failed to validate the attributes for object [%s:%s]",
										obj.getType(), obj.getId()), explode);
								return null;
							}
						}, id.getId());
						qr.query("select * from dctm_property where object_id = ?", new ResultSetHandler<Void>() {
							@Override
							public Void handle(ResultSet rs) throws SQLException {
								boolean explode = true;
								while (rs.next()) {
									explode = false;
									final String objectId = rs.getString("object_id");
									final String name = rs.getString("name");
									final CmsDataType dataType = CmsDataType.valueOf(rs.getString("data_type"));
									final boolean repeating = rs.getBoolean("repeating");
									final CmsProperty property = obj.getProperty(name);
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
								Assert.assertFalse(
									String.format("Failed to validate the attributes for object [%s:%s]",
										obj.getType(), obj.getId()), explode);
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

	/**
	 * Test method for {@link com.delta.cmsmf.cms.CmsObject#getType()}.
	 */
	@Test
	public void testGetType() throws Throwable {
		for (CmsObjectType t : CmsObjectType.values()) {
			CmsObject<?> obj = t.newInstance();
			Assert.assertEquals(t, obj.getType());
		}
	}

	/**
	 * Test method for {@link com.delta.cmsmf.cms.CmsObject#getDfClass()}.
	 */
	@Test
	public void testGetDfClass() throws Throwable {
		for (CmsObjectType t : CmsObjectType.values()) {
			CmsObject<?> obj = t.newInstance();
			Assert.assertEquals(t.getDfClass(), obj.getDfClass());
		}
	}

	/**
	 * Test method for {@link com.delta.cmsmf.cms.CmsObject#getAttributeCount()}.
	 */
	@Test
	public void testGetAttributeCount() throws Throwable {
		for (CmsObjectType t : CmsObjectType.values()) {
			CmsObject<?> obj = t.newInstance();
			int count = 0;
			for (int i = 0; i < 100; i++) {
				CmsAttribute attribute = new CmsAttribute(String.format("attribute-%03d", i), CmsDataType.DF_STRING,
					"", 0, false, false);
				obj.setAttribute(attribute);
				count++;
			}
			Assert.assertEquals(count, obj.getAttributeCount());
		}
	}

	/**
	 * Test method for {@link com.delta.cmsmf.cms.CmsObject#getAttributeNames()}.
	 */
	@Test
	public void testGetAttributeNames() throws Throwable {
		Set<String> names = new HashSet<String>();
		for (CmsObjectType t : CmsObjectType.values()) {
			CmsObject<?> obj = t.newInstance();
			for (int i = 0; i < 100; i++) {
				final String name = String.format("attribute-%03d", i);
				CmsAttribute attribute = new CmsAttribute(name, CmsDataType.DF_STRING, "", 0, false, false);
				obj.setAttribute(attribute);
				names.add(name);
			}
			Assert.assertEquals(names, obj.getAttributeNames());
		}
	}

	/**
	 * Test method for {@link com.delta.cmsmf.cms.CmsObject#getAttribute(java.lang.String)}.
	 */
	@Test
	public void testGetSetAttribute() throws Throwable {
		for (CmsObjectType t : CmsObjectType.values()) {
			CmsObject<?> obj = t.newInstance();
			for (int i = 0; i < 100; i++) {
				final String name = String.format("attribute-%03d", i);
				final String value = String.format("value-%08x", i);
				CmsAttribute attribute = new CmsAttribute(name, CmsDataType.DF_STRING, "", 0, false, false);
				attribute.setValue(DfValueFactory.newStringValue(value));
				obj.setAttribute(attribute);
			}
			for (int i = 0; i < 100; i++) {
				final String name = String.format("attribute-%03d", i);
				final String value = String.format("value-%08x", i);
				CmsAttribute attribute = obj.getAttribute(name);
				Assert.assertEquals(name, attribute.getName());
				Assert.assertEquals(value, attribute.getValue().asString());
			}
		}
	}

	/**
	 * Test method for {@link com.delta.cmsmf.cms.CmsObject#removeAttribute(java.lang.String)}.
	 */
	@Test
	public void testRemoveAttribute() throws Throwable {
		for (CmsObjectType t : CmsObjectType.values()) {
			CmsObject<?> obj = t.newInstance();
			for (int i = 0; i < 100; i++) {
				final String name = String.format("attribute-%03d", i);
				final String value = String.format("value-%08x", i);
				CmsAttribute attribute = new CmsAttribute(name, CmsDataType.DF_STRING, "", 0, false, false);
				attribute.setValue(DfValueFactory.newStringValue(value));
				obj.setAttribute(attribute);
			}
			for (int i = 0; i < 100; i++) {
				final String name = String.format("attribute-%03d", i);
				CmsAttribute attribute = obj.getAttribute(name);
				Assert.assertNotNull(attribute);
				obj.removeAttribute(name);
				Assert.assertNull(obj.getAttribute(name));
			}
		}
	}

	/**
	 * Test method for {@link com.delta.cmsmf.cms.CmsObject#getAllAttributes()}.
	 */
	@Test
	public void testGetAllAttributes() throws Throwable {
		for (CmsObjectType t : CmsObjectType.values()) {
			CmsObject<?> obj = t.newInstance();
			Map<String, String> values = new HashMap<String, String>();
			int count = 0;
			for (int i = 0; i < 100; i++) {
				final String name = String.format("attribute-%03d", i);
				final String value = String.format("value-%08x", i);
				values.put(name, value);
				CmsAttribute attribute = new CmsAttribute(name, CmsDataType.DF_STRING, "", 0, false, false);
				attribute.setValue(DfValueFactory.newStringValue(value));
				obj.setAttribute(attribute);
				count++;
			}
			Collection<CmsAttribute> attributes = obj.getAllAttributes();
			Assert.assertEquals(count, attributes.size());
			for (CmsAttribute attribute : attributes) {
				String value = values.get(attribute.getName());
				Assert.assertNotNull(value);
				Assert.assertEquals(value, attribute.getValue().asString());
			}
		}
	}

	/**
	 * Test method for {@link com.delta.cmsmf.cms.CmsObject#getPropertyCount()}.
	 */
	@Test
	public void testGetPropertyCount() throws Throwable {
		for (CmsObjectType t : CmsObjectType.values()) {
			CmsObject<?> obj = t.newInstance();
			int count = 0;
			for (int i = 0; i < 100; i++) {
				CmsProperty property = new CmsProperty(String.format("property-%03d", i), CmsDataType.DF_STRING, false);
				obj.setProperty(property);
				count++;
			}
			Assert.assertEquals(count, obj.getPropertyCount());
		}
	}

	/**
	 * Test method for {@link com.delta.cmsmf.cms.CmsObject#getPropertyNames()}.
	 */
	@Test
	public void testGetPropertyNames() throws Throwable {
		Set<String> names = new HashSet<String>();
		for (CmsObjectType t : CmsObjectType.values()) {
			CmsObject<?> obj = t.newInstance();
			for (int i = 0; i < 100; i++) {
				final String name = String.format("property-%03d", i);
				CmsProperty property = new CmsProperty(name, CmsDataType.DF_STRING, false);
				obj.setProperty(property);
				names.add(name);
			}
			Assert.assertEquals(names, obj.getPropertyNames());
		}
	}

	/**
	 * Test method for {@link com.delta.cmsmf.cms.CmsObject#getProperty(java.lang.String)}.
	 */
	@Test
	public void testGetSetProperty() throws Throwable {
		for (CmsObjectType t : CmsObjectType.values()) {
			CmsObject<?> obj = t.newInstance();
			for (int i = 0; i < 100; i++) {
				final String name = String.format("property-%03d", i);
				final String value = String.format("value-%08x", i);
				CmsProperty property = new CmsProperty(name, CmsDataType.DF_STRING, false);
				property.setValue(DfValueFactory.newStringValue(value));
				obj.setProperty(property);
			}
			for (int i = 0; i < 100; i++) {
				final String name = String.format("property-%03d", i);
				final String value = String.format("value-%08x", i);
				CmsProperty property = obj.getProperty(name);
				Assert.assertEquals(name, property.getName());
				Assert.assertEquals(value, property.getValue().asString());
			}
		}
	}

	/**
	 * Test method for {@link com.delta.cmsmf.cms.CmsObject#removeProperty(java.lang.String)}.
	 */
	@Test
	public void testRemoveProperty() throws Throwable {
		for (CmsObjectType t : CmsObjectType.values()) {
			CmsObject<?> obj = t.newInstance();
			for (int i = 0; i < 100; i++) {
				final String name = String.format("property-%03d", i);
				final String value = String.format("value-%08x", i);
				CmsProperty property = new CmsProperty(name, CmsDataType.DF_STRING, false);
				property.setValue(DfValueFactory.newStringValue(value));
				obj.setProperty(property);
			}
			for (int i = 0; i < 100; i++) {
				final String name = String.format("property-%03d", i);
				CmsProperty property = obj.getProperty(name);
				Assert.assertNotNull(property);
				obj.removeProperty(name);
				Assert.assertNull(obj.getProperty(name));
			}
		}
	}

	/**
	 * Test method for {@link com.delta.cmsmf.cms.CmsObject#getAllProperties()}.
	 */
	@Test
	public void testGetAllProperties() throws Throwable {
		for (CmsObjectType t : CmsObjectType.values()) {
			CmsObject<?> obj = t.newInstance();
			Map<String, String> values = new HashMap<String, String>();
			int count = 0;
			for (int i = 0; i < 100; i++) {
				final String name = String.format("property-%03d", i);
				final String value = String.format("value-%08x", i);
				values.put(name, value);
				CmsProperty property = new CmsProperty(name, CmsDataType.DF_STRING, false);
				property.setValue(DfValueFactory.newStringValue(value));
				obj.setProperty(property);
				count++;
			}
			Collection<CmsProperty> propertys = obj.getAllProperties();
			Assert.assertEquals(count, propertys.size());
			for (CmsProperty property : propertys) {
				String value = values.get(property.getName());
				Assert.assertNotNull(value);
				Assert.assertEquals(value, property.getValue().asString());
			}
		}
	}

	/**
	 * Test method for
	 * {@link com.delta.cmsmf.cms.CmsObject#loadFromCMS(com.documentum.fc.client.IDfPersistentObject)}
	 * .
	 */
	@Test
	public void testLoadFromCMS() {
	}

	/**
	 * Test method for {@link com.delta.cmsmf.cms.CmsObject#saveToCMS(CmsTransferContext)} .
	 */
	@Test
	public void testSaveToCMS() {
	}

	/**
	 * Test method for
	 * {@link com.delta.cmsmf.cms.CmsObject#getAttributeHandler(com.documentum.fc.common.IDfAttr)}.
	 */
	@Test
	public void testGetAttributeHandlerIDfAttr() {
	}

	/**
	 * Test method for
	 * {@link com.delta.cmsmf.cms.CmsObject#getAttributeHandler(com.delta.cmsmf.cms.CmsAttribute)}.
	 */
	@Test
	public void testGetAttributeHandlerCmsAttribute() {
	}

	/**
	 * Test method for
	 * {@link com.delta.cmsmf.cms.CmsObject#getAttributeHandler(com.delta.cmsmf.cms.CmsDataType, java.lang.String)}
	 * .
	 */
	@Test
	public void testGetAttributeHandlerCmsDataTypeString() {
	}

	/**
	 * Test method for
	 * {@link com.delta.cmsmf.cms.CmsObject#castObject(com.documentum.fc.client.IDfPersistentObject)}
	 * .
	 */
	@Test
	public void testCastObject() {
	}

	/**
	 * Test method for {@link com.delta.cmsmf.cms.CmsObject#locateInCms(CmsTransferContext)}.
	 */
	@Test
	public void testLocateInCms() {
	}

	/**
	 * Test method for {@link com.delta.cmsmf.cms.CmsObject#getId()}.
	 */
	@Test
	public void testGetId() {
	}

	/**
	 * Test method for
	 * {@link com.delta.cmsmf.cms.CmsObject#copyAttributeToObject(java.lang.String, com.documentum.fc.client.IDfPersistentObject)}
	 * .
	 */
	@Test
	public void testCopyAttributeToObjectStringT() {
	}

	/**
	 * Test method for
	 * {@link com.delta.cmsmf.cms.CmsObject#copyAttributeToObject(com.delta.cmsmf.cms.CmsAttribute, com.documentum.fc.client.IDfPersistentObject)}
	 * .
	 */
	@Test
	public void testCopyAttributeToObjectCmsAttributeT() {
	}

	/**
	 * Test method for
	 * {@link com.delta.cmsmf.cms.CmsObject#setAttributeOnObject(java.lang.String, com.documentum.fc.common.IDfValue, com.documentum.fc.client.IDfPersistentObject)}
	 * .
	 */
	@Test
	public void testSetAttributeOnObjectStringIDfValueT() {
	}

	/**
	 * Test method for
	 * {@link com.delta.cmsmf.cms.CmsObject#setAttributeOnObject(java.lang.String, java.util.Collection, com.documentum.fc.client.IDfPersistentObject)}
	 * .
	 */
	@Test
	public void testSetAttributeOnObjectStringCollectionOfIDfValueT() {
	}

	/**
	 * Test method for
	 * {@link com.delta.cmsmf.cms.CmsObject#setAttributeOnObject(com.delta.cmsmf.cms.CmsAttribute, com.documentum.fc.common.IDfValue, com.documentum.fc.client.IDfPersistentObject)}
	 * .
	 */
	@Test
	public void testSetAttributeOnObjectCmsAttributeIDfValueT() {
	}

	/**
	 * Test method for
	 * {@link com.delta.cmsmf.cms.CmsObject#setAttributeOnObject(com.delta.cmsmf.cms.CmsAttribute, java.util.Collection, com.documentum.fc.client.IDfPersistentObject)}
	 * .
	 */
	@Test
	public void testSetAttributeOnObjectCmsAttributeCollectionOfIDfValueT() {
	}

	/**
	 * Test method for
	 * {@link com.delta.cmsmf.cms.CmsObject#clearAttributeFromObject(java.lang.String, com.documentum.fc.client.IDfPersistentObject)}
	 * .
	 */
	@Test
	public void testClearAttributeFromObjectStringT() {
	}

	/**
	 * Test method for
	 * {@link com.delta.cmsmf.cms.CmsObject#clearAttributeFromObject(com.delta.cmsmf.cms.CmsAttribute, com.documentum.fc.client.IDfPersistentObject)}
	 * .
	 */
	@Test
	public void testClearAttributeFromObjectCmsAttributeT() {
	}

	/**
	 * Test method for
	 * {@link com.delta.cmsmf.cms.CmsObject#clearAttributeFromObject(com.documentum.fc.common.IDfAttr, com.documentum.fc.client.IDfPersistentObject)}
	 * .
	 */
	@Test
	public void testClearAttributeFromObjectIDfAttrT() {
	}

	/**
	 * Test method for
	 * {@link com.delta.cmsmf.cms.CmsObject#clearAttributeFromObject(java.lang.String, com.delta.cmsmf.cms.CmsDataType, boolean, com.documentum.fc.client.IDfPersistentObject)}
	 * .
	 */
	@Test
	public void testClearAttributeFromObjectStringCmsDataTypeBooleanT() {
	}

	/**
	 * Test method for
	 * {@link com.delta.cmsmf.cms.CmsObject#updateSystemAttributes(IDfPersistentObject)} .
	 */
	@Test
	public void testUpdateSystemAttributes() {
	}

	/**
	 * Test method for
	 * {@link com.delta.cmsmf.cms.CmsObject#updateVStamp(int, com.documentum.fc.client.IDfPersistentObject)}
	 * .
	 */
	@Test
	public void testUpdateVStampIntT() {
	}

	/**
	 * Test method for
	 * {@link com.delta.cmsmf.cms.CmsObject#updateVStamp(java.lang.String, int, com.documentum.fc.client.IDfPersistentObject)}
	 * .
	 */
	@Test
	public void testUpdateVStampStringIntT() {
	}
}