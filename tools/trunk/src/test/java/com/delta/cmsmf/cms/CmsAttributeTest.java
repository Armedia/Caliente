/**
 *
 */

package com.delta.cmsmf.cms;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.junit.Assert;
import org.junit.Test;

import com.delta.cmsmf.cms.storage.CmsObjectStore;
import com.delta.cmsmf.utils.DfUtils;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfValue;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class CmsAttributeTest extends AbstractTest {

	@Test
	public void testCmsAttributeResultSet() throws Throwable {
		try {
			new CmsAttribute(null);
			Assert.fail("Did not fail with a null ResultSet");
		} catch (IllegalArgumentException e) {
			// All is well
		}
		final IDfSession session = acquireSourceSession();
		final QueryRunner qr = new QueryRunner(getDataSource());
		final CmsObjectStore store = new CmsObjectStore(getDataSource(), true);
		final CmsFileSystem fs = new DefaultCmsFileSystem(getFsDir());
		try {
			IDfCollection collection = DfUtils.executeQuery(session,
				"select r_object_id from dm_document where folder('/CMSMFTests', DESCEND)", IDfQuery.DF_EXECREAD_QUERY);
			try {
				final CmsObjectType type = CmsObjectType.DOCUMENT;
				final int max = 3;
				int c = 0;
				while (collection.next()) {
					if (c > max) {
						break;
					}
					IDfId id = collection.getId("r_object_id");
					IDfPersistentObject dfObj = session.getObject(id);
					Assert.assertNotNull(dfObj);
					final CmsObject<?> cmsObj = type.newInstance();
					if (!cmsObj.loadFromCMS(dfObj)) {
						// Unsupported object
						continue;
					}
					CmsTransferContext ctx = new DefaultTransferContext(cmsObj.getId(), session, store, fs);
					store.serializeObject(cmsObj, ctx);

					for (final CmsAttribute att : cmsObj.getAllAttributes()) {
						qr.query("select * from dctm_attribute where object_id = ? and name = ?",
							new ResultSetHandler<Void>() {
							@Override
							public Void handle(ResultSet rs) throws SQLException {
								if (!rs.next()) {
									Assert.fail(String.format(
											"No data found for attribute [%s] for object [%s:%s]", att.getName(),
											cmsObj.getType(), cmsObj.getId()));
								}
								final CmsAttribute actual = new CmsAttribute(rs);
								try {
									actual.loadValues(null);
									Assert.fail("LoadValues did not fail with a null ResultSet");
								} catch (IllegalArgumentException e) {
									// All is well
								}
								Assert.assertEquals(att.getType(), actual.getType());
								Assert.assertEquals(att.getName(), actual.getName());
								Assert.assertEquals(att.isRepeating(), actual.isRepeating());
								Assert.assertEquals(att.getId(), actual.getId());
								Assert.assertEquals(att.isQualifiable(), actual.isQualifiable());
								Assert.assertEquals(att.getLength(), actual.getLength());

								qr.query(
										"select * from dctm_attribute_value where object_id = ? and name = ? order by value_number",
										new ResultSetHandler<Void>() {
											@Override
											public Void handle(ResultSet rs) throws SQLException {
												actual.loadValues(rs);
												return null;
											}
									}, cmsObj.getId(), att.getName());

								Assert.assertEquals(att.getValueCount(), actual.getValueCount());
								for (int i = 0; i < att.getValueCount(); i++) {
									IDfValue vExp = att.getValue(i);
									IDfValue vAct = actual.getValue(i);
									CmsDataType type = att.getType();
									Assert.assertEquals(type.getValue(vExp), type.getValue(vAct));
								}
								return null;
							}
						}, cmsObj.getId(), att.getName());
					}
					c++;
				}
			} finally {
				closeQuietly(collection);
			}

		} finally {
			releaseSourceSession(session);
		}
	}

	@Test
	public void testCmsAttributeIDfAttrIDfPersistentObject() throws Throwable {
		final IDfSession session = acquireSourceSession();
		try {
			IDfCollection collection = DfUtils.executeQuery(session,
				"select r_object_id from dm_document where folder('/CMSMFTests', DESCEND)", IDfQuery.DF_EXECREAD_QUERY);
			try {
				final int max = 3;
				int c = 0;
				while (collection.next()) {
					if (c > max) {
						break;
					}
					IDfId id = collection.getId("r_object_id");
					IDfPersistentObject dfObj = session.getObject(id);
					Assert.assertNotNull(dfObj);
					final int attCount = dfObj.getAttrCount();
					for (int i = 0; i < attCount; i++) {
						final IDfAttr expected = dfObj.getAttr(i);
						final CmsDataType expectedType = CmsDataType.fromAttribute(expected);
						try {
							new CmsAttribute(null, dfObj);
							Assert.fail("Did not fail with a null attribute");
						} catch (IllegalArgumentException e) {
							// All is well
						}
						try {
							new CmsAttribute(null, (Collection<IDfValue>) null);
							Assert.fail("Did not fail with a null attribute");
						} catch (IllegalArgumentException e) {
							// All is well
						}
						try {
							new CmsAttribute(null, (IDfValue[]) null);
							Assert.fail("Did not fail with a null attribute");
						} catch (IllegalArgumentException e) {
							// All is well
						}
						try {
							new CmsAttribute(null, (IDfValue) null);
							Assert.fail("Did not fail with a null attribute");
						} catch (IllegalArgumentException e) {
							// All is well
						}
						CmsAttribute actual = new CmsAttribute(expected, IDfPersistentObject.class.cast(null));
						Assert.assertEquals(expectedType, actual.getType());
						Assert.assertEquals(expected.getName(), actual.getName());
						Assert.assertEquals(expected.isRepeating(), actual.isRepeating());
						Assert.assertEquals(expected.getId(), actual.getId());
						Assert.assertEquals(expected.isQualifiable(), actual.isQualifiable());
						Assert.assertEquals(expected.getLength(), actual.getLength());
						if (actual.isRepeating()) {
							Assert.assertEquals(0, actual.getValueCount());
						} else {
							Assert.assertEquals(1, actual.getValueCount());
							Assert.assertEquals(expectedType.getValue(expectedType.getNullValue()),
								expectedType.getValue(actual.getValue()));
						}
						actual = new CmsAttribute(expected);
						Assert.assertEquals(expectedType, actual.getType());
						Assert.assertEquals(expected.getName(), actual.getName());
						Assert.assertEquals(expected.isRepeating(), actual.isRepeating());
						Assert.assertEquals(expected.getId(), actual.getId());
						Assert.assertEquals(expected.isQualifiable(), actual.isQualifiable());
						Assert.assertEquals(expected.getLength(), actual.getLength());
						if (actual.isRepeating()) {
							Assert.assertEquals(0, actual.getValueCount());
						} else {
							Assert.assertEquals(1, actual.getValueCount());
							Assert.assertEquals(expectedType.getValue(expectedType.getNullValue()),
								expectedType.getValue(actual.getValue()));
						}
						actual = new CmsAttribute(expected, (IDfValue) null);
						Assert.assertEquals(expectedType, actual.getType());
						Assert.assertEquals(expected.getName(), actual.getName());
						Assert.assertEquals(expected.isRepeating(), actual.isRepeating());
						Assert.assertEquals(expected.getId(), actual.getId());
						Assert.assertEquals(expected.isQualifiable(), actual.isQualifiable());
						Assert.assertEquals(expected.getLength(), actual.getLength());
						Assert.assertEquals(1, actual.getValueCount());
						Assert.assertEquals(expectedType.getValue(expectedType.getNullValue()),
							expectedType.getValue(actual.getValue(0)));
						actual = new CmsAttribute(expected, (IDfValue[]) null);
						Assert.assertEquals(expectedType, actual.getType());
						Assert.assertEquals(expected.getName(), actual.getName());
						Assert.assertEquals(expected.isRepeating(), actual.isRepeating());
						Assert.assertEquals(expected.getId(), actual.getId());
						Assert.assertEquals(expected.isQualifiable(), actual.isQualifiable());
						Assert.assertEquals(expected.getLength(), actual.getLength());
						if (actual.isRepeating()) {
							Assert.assertEquals(0, actual.getValueCount());
						} else {
							Assert.assertEquals(1, actual.getValueCount());
							Assert.assertEquals(expectedType.getValue(expectedType.getNullValue()),
								expectedType.getValue(actual.getValue()));
						}
						actual = new CmsAttribute(expected, (Collection<IDfValue>) null);
						Assert.assertEquals(expectedType, actual.getType());
						Assert.assertEquals(expected.getName(), actual.getName());
						Assert.assertEquals(expected.isRepeating(), actual.isRepeating());
						Assert.assertEquals(expected.getId(), actual.getId());
						Assert.assertEquals(expected.isQualifiable(), actual.isQualifiable());
						Assert.assertEquals(expected.getLength(), actual.getLength());
						if (actual.isRepeating()) {
							Assert.assertEquals(0, actual.getValueCount());
						} else {
							Assert.assertEquals(1, actual.getValueCount());
							Assert.assertEquals(expectedType.getValue(expectedType.getNullValue()),
								expectedType.getValue(actual.getValue()));
						}

						final int valueCount = dfObj.getValueCount(expected.getName());

						actual = new CmsAttribute(expected, DfValueFactory.getAllRepeatingValues(expected, dfObj));
						Assert.assertEquals(expectedType, actual.getType());
						Assert.assertEquals(expected.getName(), actual.getName());
						Assert.assertEquals(expected.isRepeating(), actual.isRepeating());
						Assert.assertEquals(expected.getId(), actual.getId());
						Assert.assertEquals(expected.isQualifiable(), actual.isQualifiable());
						Assert.assertEquals(expected.getLength(), actual.getLength());
						Assert.assertEquals(valueCount, actual.getValueCount());
						for (int v = 0; v < valueCount; v++) {
							IDfValue vExp = dfObj.getRepeatingValue(expected.getName(), v);
							IDfValue vAct = actual.getValue(v);
							Assert.assertEquals(expectedType.getValue(vExp), expectedType.getValue(vAct));
						}

						actual = new CmsAttribute(expected, DfValueFactory.getAllRepeatingValues(expected, dfObj)
							.toArray(CmsProperty.NO_VALUES));
						Assert.assertEquals(expectedType, actual.getType());
						Assert.assertEquals(expected.getName(), actual.getName());
						Assert.assertEquals(expected.isRepeating(), actual.isRepeating());
						Assert.assertEquals(expected.getId(), actual.getId());
						Assert.assertEquals(expected.isQualifiable(), actual.isQualifiable());
						Assert.assertEquals(expected.getLength(), actual.getLength());
						Assert.assertEquals(valueCount, actual.getValueCount());
						for (int v = 0; v < valueCount; v++) {
							IDfValue vExp = dfObj.getRepeatingValue(expected.getName(), v);
							IDfValue vAct = actual.getValue(v);
							Assert.assertEquals(expectedType.getValue(vExp), expectedType.getValue(vAct));
						}

						actual = new CmsAttribute(expected, dfObj);
						Assert.assertEquals(expectedType, actual.getType());
						Assert.assertEquals(expected.getName(), actual.getName());
						Assert.assertEquals(expected.isRepeating(), actual.isRepeating());
						Assert.assertEquals(expected.getId(), actual.getId());
						Assert.assertEquals(expected.isQualifiable(), actual.isQualifiable());
						Assert.assertEquals(expected.getLength(), actual.getLength());
						Assert.assertEquals(valueCount, actual.getValueCount());
						for (int v = 0; v < valueCount; v++) {
							IDfValue vExp = dfObj.getRepeatingValue(expected.getName(), v);
							IDfValue vAct = actual.getValue(v);
							Assert.assertEquals(expectedType.getValue(vExp), expectedType.getValue(vAct));
						}
					}
					c++;
				}
			} finally {
				closeQuietly(collection);
			}
		} finally {
			releaseSourceSession(session);
		}
	}

	@Test
	public void testCmsAttributeIDfAttrIDfValueArray() throws Throwable {
		final IDfSession session = acquireSourceSession();
		try {
			IDfCollection collection = DfUtils.executeQuery(session,
				"select r_object_id from dm_document where folder('/CMSMFTests', DESCEND)", IDfQuery.DF_EXECREAD_QUERY);
			try {
				final int max = 3;
				int c = 0;
				while (collection.next()) {
					if (c > max) {
						break;
					}
					IDfId id = collection.getId("r_object_id");
					IDfPersistentObject dfObj = session.getObject(id);
					Assert.assertNotNull(dfObj);
					final int attCount = dfObj.getAttrCount();
					for (int i = 0; i < attCount; i++) {
						final IDfAttr expected = dfObj.getAttr(i);
						final int valueCount = dfObj.getValueCount(expected.getName());
						final IDfValue[] values = new IDfValue[valueCount];
						for (int v = 0; v < valueCount; v++) {
							values[v] = dfObj.getRepeatingValue(expected.getName(), v);
						}

						CmsAttribute actual = new CmsAttribute(expected, values);
						final CmsDataType expectedType = CmsDataType.fromAttribute(expected);
						Assert.assertEquals(expectedType, actual.getType());
						Assert.assertEquals(expected.getName(), actual.getName());
						Assert.assertEquals(expected.isRepeating(), actual.isRepeating());
						Assert.assertEquals(expected.getId(), actual.getId());
						Assert.assertEquals(expected.isQualifiable(), actual.isQualifiable());
						Assert.assertEquals(expected.getLength(), actual.getLength());
						Assert.assertEquals(valueCount, actual.getValueCount());
						for (int v = 0; v < valueCount; v++) {
							IDfValue vExp = dfObj.getRepeatingValue(expected.getName(), v);
							IDfValue vAct = actual.getValue(v);
							Assert.assertEquals(expectedType.getValue(vExp), expectedType.getValue(vAct));
						}
					}
					c++;
				}
			} finally {
				closeQuietly(collection);
			}
		} finally {
			releaseSourceSession(session);
		}
	}

	@Test
	public void testCmsAttributeIDfAttrCollectionOfIDfValue() throws Throwable {
		final IDfSession session = acquireSourceSession();
		try {
			IDfCollection collection = DfUtils.executeQuery(session,
				"select r_object_id from dm_document where folder('/CMSMFTests', DESCEND)", IDfQuery.DF_EXECREAD_QUERY);
			try {
				final int max = 3;
				int c = 0;
				while (collection.next()) {
					if (c > max) {
						break;
					}
					IDfId id = collection.getId("r_object_id");
					IDfPersistentObject dfObj = session.getObject(id);
					Assert.assertNotNull(dfObj);
					final int attCount = dfObj.getAttrCount();
					for (int i = 0; i < attCount; i++) {
						final IDfAttr expected = dfObj.getAttr(i);
						final int valueCount = dfObj.getValueCount(expected.getName());
						final Collection<IDfValue> values = new ArrayList<IDfValue>();
						for (int v = 0; v < valueCount; v++) {
							values.add(dfObj.getRepeatingValue(expected.getName(), v));
						}

						CmsAttribute actual = new CmsAttribute(expected, values);
						final CmsDataType expectedType = CmsDataType.fromAttribute(expected);
						Assert.assertEquals(expectedType, actual.getType());
						Assert.assertEquals(expected.getName(), actual.getName());
						Assert.assertEquals(expected.isRepeating(), actual.isRepeating());
						Assert.assertEquals(expected.getId(), actual.getId());
						Assert.assertEquals(expected.isQualifiable(), actual.isQualifiable());
						Assert.assertEquals(expected.getLength(), actual.getLength());
						Assert.assertEquals(valueCount, actual.getValueCount());
						for (int v = 0; v < valueCount; v++) {
							IDfValue vExp = dfObj.getRepeatingValue(expected.getName(), v);
							IDfValue vAct = actual.getValue(v);
							Assert.assertEquals(expectedType.getValue(vExp), expectedType.getValue(vAct));
						}
					}
					c++;
				}
			} finally {
				closeQuietly(collection);
			}
		} finally {
			releaseSourceSession(session);
		}
	}

	@Test
	public void testCmsAttributeStringCmsDataTypeStringIntBooleanBooleanIDfValueArray() throws Throwable {
		final IDfSession session = acquireSourceSession();
		try {
			IDfCollection collection = DfUtils.executeQuery(session,
				"select r_object_id from dm_document where folder('/CMSMFTests', DESCEND)", IDfQuery.DF_EXECREAD_QUERY);
			try {
				final int max = 3;
				int c = 0;
				while (collection.next()) {
					if (c > max) {
						break;
					}
					IDfId id = collection.getId("r_object_id");
					IDfPersistentObject dfObj = session.getObject(id);
					Assert.assertNotNull(dfObj);
					final int attCount = dfObj.getAttrCount();
					for (int i = 0; i < attCount; i++) {
						final IDfAttr expected = dfObj.getAttr(i);
						final int valueCount = dfObj.getValueCount(expected.getName());
						final IDfValue[] values = new IDfValue[valueCount];
						for (int v = 0; v < valueCount; v++) {
							values[v] = dfObj.getRepeatingValue(expected.getName(), v);
						}

						CmsAttribute actual = new CmsAttribute(expected.getName(), CmsDataType.fromAttribute(expected),
							expected.getId(), expected.getLength(), expected.isRepeating(), expected.isQualifiable(),
							values);
						final CmsDataType expectedType = CmsDataType.fromAttribute(expected);
						Assert.assertEquals(expectedType, actual.getType());
						Assert.assertEquals(expected.getName(), actual.getName());
						Assert.assertEquals(expected.isRepeating(), actual.isRepeating());
						Assert.assertEquals(expected.getId(), actual.getId());
						Assert.assertEquals(expected.isQualifiable(), actual.isQualifiable());
						Assert.assertEquals(expected.getLength(), actual.getLength());
						Assert.assertEquals(valueCount, actual.getValueCount());
						for (int v = 0; v < valueCount; v++) {
							IDfValue vExp = dfObj.getRepeatingValue(expected.getName(), v);
							IDfValue vAct = actual.getValue(v);
							Assert.assertEquals(expectedType.getValue(vExp), expectedType.getValue(vAct));
						}
					}
					c++;
				}
			} finally {
				closeQuietly(collection);
			}
		} finally {
			releaseSourceSession(session);
		}
	}

	@Test
	public void testCmsAttributeStringCmsDataTypeStringIntBooleanBooleanCollectionOfIDfValue() throws Throwable {
		final IDfSession session = acquireSourceSession();
		try {
			IDfCollection collection = DfUtils.executeQuery(session,
				"select r_object_id from dm_document where folder('/CMSMFTests', DESCEND)", IDfQuery.DF_EXECREAD_QUERY);
			try {
				final int max = 3;
				int c = 0;
				while (collection.next()) {
					if (c > max) {
						break;
					}
					IDfId id = collection.getId("r_object_id");
					IDfPersistentObject dfObj = session.getObject(id);
					Assert.assertNotNull(dfObj);
					final int attCount = dfObj.getAttrCount();
					for (int i = 0; i < attCount; i++) {
						final IDfAttr expected = dfObj.getAttr(i);
						final int valueCount = dfObj.getValueCount(expected.getName());
						final Collection<IDfValue> values = new ArrayList<IDfValue>();
						for (int v = 0; v < valueCount; v++) {
							values.add(dfObj.getRepeatingValue(expected.getName(), v));
						}

						CmsAttribute actual = new CmsAttribute(expected.getName(), CmsDataType.fromAttribute(expected),
							expected.getId(), expected.getLength(), expected.isRepeating(), expected.isQualifiable(),
							values);
						final CmsDataType expectedType = CmsDataType.fromAttribute(expected);
						Assert.assertEquals(expectedType, actual.getType());
						Assert.assertEquals(expected.getName(), actual.getName());
						Assert.assertEquals(expected.isRepeating(), actual.isRepeating());
						Assert.assertEquals(expected.getId(), actual.getId());
						Assert.assertEquals(expected.isQualifiable(), actual.isQualifiable());
						Assert.assertEquals(expected.getLength(), actual.getLength());
						Assert.assertEquals(valueCount, actual.getValueCount());
						for (int v = 0; v < valueCount; v++) {
							IDfValue vExp = dfObj.getRepeatingValue(expected.getName(), v);
							IDfValue vAct = actual.getValue(v);
							Assert.assertEquals(expectedType.getValue(vExp), expectedType.getValue(vAct));
						}
					}
					c++;
				}
			} finally {
				closeQuietly(collection);
			}
		} finally {
			releaseSourceSession(session);
		}
	}

	@Test
	public void testIsQualifiable() {
		CmsAttribute actual = null;

		actual = new CmsAttribute("name", CmsDataType.DF_BOOLEAN, "id", 0, false, true);
		Assert.assertTrue(actual.isQualifiable());
		actual = new CmsAttribute("name", CmsDataType.DF_BOOLEAN, "id", 0, false, false);
		Assert.assertFalse(actual.isQualifiable());
	}

	@Test
	public void testGetId() {
		CmsAttribute actual = null;

		try {
			actual = new CmsAttribute("name", CmsDataType.DF_BOOLEAN, null, 0, false, false);
			Assert.fail("Did not fail with null ID");
		} catch (IllegalArgumentException e) {
			// all is well
		}

		String uuid = UUID.randomUUID().toString();
		actual = new CmsAttribute("name", CmsDataType.DF_BOOLEAN, uuid, 0, false, false);
		Assert.assertEquals(uuid, actual.getId());
	}

	@Test
	public void testGetLength() {
		CmsAttribute actual = null;
		for (int i = 0; i < 100; i++) {
			actual = new CmsAttribute("name", CmsDataType.DF_BOOLEAN, "", i, false, false);
			Assert.assertEquals(i, actual.getLength());
		}
	}

	@Test
	public void testIsSameCmsAttribute() {
		String[] strings = {
			UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString()
		};
		int[] lengths = {
			0, 1, 2, 3, 4
		};
		boolean[] booleans = {
			false, true
		};
		for (String name : strings) {
			for (CmsDataType type : CmsDataType.values()) {
				if (type == CmsDataType.DF_UNDEFINED) {
					continue;
				}
				for (String id : strings) {
					for (int length : lengths) {
						for (boolean r : booleans) {
							for (boolean q : booleans) {
								for (String vA : strings) {
									final IDfValue a = DfValueFactory.newStringValue(vA);
									final CmsAttribute attA = new CmsAttribute(name, type, id, length, r, q,
										Collections.singleton(a));
									Assert.assertTrue(attA.isSame(attA));
									for (String vB : strings) {
										final IDfValue b = DfValueFactory.newStringValue(vB);
										final CmsAttribute attB = new CmsAttribute(name, type, id, length, r, q,
											Collections.singleton(b));
										Assert.assertTrue(attA.isSame(attB));
										Assert.assertTrue(attB.isSame(attA));
									}
								}
							}
						}
					}
				}
			}
		}
		for (String nameA : strings) {
			for (CmsDataType typeA : CmsDataType.values()) {
				if (typeA == CmsDataType.DF_UNDEFINED) {
					continue;
				}
				for (String idA : strings) {
					for (int lengthA : lengths) {
						for (boolean rA : booleans) {
							for (boolean qA : booleans) {
								for (String vA : strings) {
									final CmsAttribute attA = new CmsAttribute(nameA, typeA, idA, lengthA, rA, qA,
										Collections.singleton(DfValueFactory.newStringValue(vA)));
									for (String nameB : strings) {
										for (CmsDataType typeB : CmsDataType.values()) {
											if (typeB == CmsDataType.DF_UNDEFINED) {
												continue;
											}
											for (String idB : strings) {
												for (int lengthB : lengths) {
													for (boolean rB : booleans) {
														for (boolean qB : booleans) {
															for (String vB : strings) {
																final CmsAttribute attB = new CmsAttribute(nameB,
																	typeB, idB, lengthB, rB, qB,
																	Collections.singleton(DfValueFactory
																		.newStringValue(vB)));
																boolean different = (!nameA.equals(nameB)
																	|| (typeA != typeB) || !idA.equals(idB)
																	|| (lengthA != lengthB) || (rA != rB) || (qA != qB));
																Assert.assertEquals(!different, attA.isSame(attB));
																Assert.assertEquals(!different, attB.isSame(attA));
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
}