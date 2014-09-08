/**
 *
 */

package com.delta.cmsmf.cms;

import org.apache.commons.dbutils.QueryRunner;
import org.junit.Assert;
import org.junit.Test;

import com.delta.cmsmf.cms.storage.CmsObjectStore;
import com.delta.cmsmf.exception.CMSMFException;
import com.documentum.com.DfClientX;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.IDfId;

/**
 * @author Diego Rivera <diego.rivera@armedia.com>
 *
 */
public class CmsObjectTest extends AbstractSqlTest {

	/**
	 * Test method for
	 * {@link com.delta.cmsmf.cms.CmsObject#CmsObject(com.delta.cmsmf.cms.CmsObjectType, java.lang.Class)}
	 * .
	 *
	 * @throws CMSMFException
	 */
	@Test
	public void testCmsObjectConstruction() throws Throwable {
		IDfSession session = acquireSession();
		try {
			IDfQuery q = new DfClientX().getQuery();
			final int max = 3;
			for (CmsObjectType t : CmsObjectType.values()) {
				CmsObject<? extends IDfPersistentObject> obj = t.newInstance();
				final String dql = String.format("select r_object_id from %s enable(optimize_top %d, return_top %d)",
					t.getDocumentumType(), max, max);
				q.setDQL(dql);
				IDfCollection results = q.execute(session, IDfQuery.DF_EXECREAD_QUERY);
				int count = 0;
				while (results.next()) {
					IDfId id = results.getId("r_object_id");
					IDfPersistentObject cmsObj = session.getObject(id);
					obj.loadFromCMS(cmsObj);
					if (++count > max) {
						break;
					}
				}
				if (count == 0) {
					Assert.fail(String.format("Did not find any objects of type [%s] to test against", t));
				}
			}
		} finally {
			releaseSession(session);
		}
	}

	/**
	 * Test method for {@link com.delta.cmsmf.cms.CmsObject#load(java.sql.ResultSet)}.
	 */
	@Test
	public void testCmsObjectPersistence() throws Throwable {
		CmsObjectStore store = new CmsObjectStore(getDataSource(), true);
		QueryRunner qr = new QueryRunner(getDataSource());
		IDfSession session = acquireSession();
		try {
			IDfQuery q = new DfClientX().getQuery();
			final int max = 3;
			for (CmsObjectType t : CmsObjectType.values()) {
				CmsObject<? extends IDfPersistentObject> obj = t.newInstance();
				final String dql = String.format("select r_object_id from %s enable(optimize_top %d, return_top %d)",
					t.getDocumentumType(), max, max);
				q.setDQL(dql);
				IDfCollection results = q.execute(session, IDfQuery.DF_EXECREAD_QUERY);
				int count = 0;
				while (results.next()) {
					IDfId id = results.getId("r_object_id");
					IDfPersistentObject cmsObj = session.getObject(id);
					obj.loadFromCMS(cmsObj);
					Assert.assertEquals(Integer.valueOf(0), qr.query(
						"select count(*) from dctm_object where object_id = ?", AbstractSqlTest.HANDLER_COUNT,
						id.getId()));
					store.serializeObject(obj);
					Assert.assertEquals(Integer.valueOf(1), qr.query(
						"select count(*) from dctm_object where object_id = ?", AbstractSqlTest.HANDLER_COUNT,
						id.getId()));
					Assert.assertEquals(Integer.valueOf(obj.getAttributeCount()), qr.query(
						"select count(*) from dctm_attribute where object_id = ?", AbstractSqlTest.HANDLER_COUNT,
						id.getId()));
					Assert.assertEquals(Integer.valueOf(obj.getPropertyCount()), qr.query(
						"select count(*) from dctm_property where object_id = ?", AbstractSqlTest.HANDLER_COUNT,
						id.getId()));
					if (++count > max) {
						break;
					}
				}
				if (count == 0) {
					Assert.fail(String.format("Did not find any objects of type [%s] to test against", t));
				}
			}
		} finally {
			releaseSession(session);
		}
	}

	/**
	 * Test method for {@link com.delta.cmsmf.cms.CmsObject#loadAttributes(java.sql.ResultSet)}.
	 */
	@Test
	public void testLoadAttributes() {
		Assert.fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.delta.cmsmf.cms.CmsObject#loadProperties(java.sql.ResultSet)}.
	 */
	@Test
	public void testLoadProperties() {
		Assert.fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.delta.cmsmf.cms.CmsObject#getType()}.
	 */
	@Test
	public void testGetType() {
		Assert.fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.delta.cmsmf.cms.CmsObject#getDfClass()}.
	 */
	@Test
	public void testGetDfClass() {
		Assert.fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.delta.cmsmf.cms.CmsObject#getAttributeCount()}.
	 */
	@Test
	public void testGetAttributeCount() {
		Assert.fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.delta.cmsmf.cms.CmsObject#getAttributeNames()}.
	 */
	@Test
	public void testGetAttributeNames() {
		Assert.fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.delta.cmsmf.cms.CmsObject#getAttribute(java.lang.String)}.
	 */
	@Test
	public void testGetAttribute() {
		Assert.fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link com.delta.cmsmf.cms.CmsObject#setAttribute(com.delta.cmsmf.cms.CmsAttribute)}.
	 */
	@Test
	public void testSetAttribute() {
		Assert.fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.delta.cmsmf.cms.CmsObject#removeAttribute(java.lang.String)}.
	 */
	@Test
	public void testRemoveAttribute() {
		Assert.fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.delta.cmsmf.cms.CmsObject#getAllAttributes()}.
	 */
	@Test
	public void testGetAllAttributes() {
		Assert.fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.delta.cmsmf.cms.CmsObject#getPropertyCount()}.
	 */
	@Test
	public void testGetPropertyCount() {
		Assert.fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.delta.cmsmf.cms.CmsObject#getPropertyNames()}.
	 */
	@Test
	public void testGetPropertyNames() {
		Assert.fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.delta.cmsmf.cms.CmsObject#getProperty(java.lang.String)}.
	 */
	@Test
	public void testGetProperty() {
		Assert.fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link com.delta.cmsmf.cms.CmsObject#setProperty(com.delta.cmsmf.cms.CmsProperty)}.
	 */
	@Test
	public void testSetProperty() {
		Assert.fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.delta.cmsmf.cms.CmsObject#removeProperty(java.lang.String)}.
	 */
	@Test
	public void testRemoveProperty() {
		Assert.fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.delta.cmsmf.cms.CmsObject#getAllProperties()}.
	 */
	@Test
	public void testGetAllProperties() {
		Assert.fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link com.delta.cmsmf.cms.CmsObject#loadFromCMS(com.documentum.fc.client.IDfPersistentObject)}
	 * .
	 */
	@Test
	public void testLoadFromCMS() {
		Assert.fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link com.delta.cmsmf.cms.CmsObject#saveToCMS(com.documentum.fc.client.IDfSession, com.delta.cmsmf.cms.CmsAttributeMapper)}
	 * .
	 */
	@Test
	public void testSaveToCMS() {
		Assert.fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link com.delta.cmsmf.cms.CmsObject#getAttributeHandler(com.documentum.fc.common.IDfAttr)}.
	 */
	@Test
	public void testGetAttributeHandlerIDfAttr() {
		Assert.fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link com.delta.cmsmf.cms.CmsObject#getAttributeHandler(com.delta.cmsmf.cms.CmsAttribute)}.
	 */
	@Test
	public void testGetAttributeHandlerCmsAttribute() {
		Assert.fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link com.delta.cmsmf.cms.CmsObject#getAttributeHandler(com.delta.cmsmf.cms.CmsDataType, java.lang.String)}
	 * .
	 */
	@Test
	public void testGetAttributeHandlerCmsDataTypeString() {
		Assert.fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link com.delta.cmsmf.cms.CmsObject#castObject(com.documentum.fc.client.IDfPersistentObject)}
	 * .
	 */
	@Test
	public void testCastObject() {
		Assert.fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link com.delta.cmsmf.cms.CmsObject#locateInCms(com.documentum.fc.client.IDfSession)}.
	 */
	@Test
	public void testLocateInCms() {
		Assert.fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.delta.cmsmf.cms.CmsObject#getId()}.
	 */
	@Test
	public void testGetId() {
		Assert.fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link com.delta.cmsmf.cms.CmsObject#copyAttributeToObject(java.lang.String, com.documentum.fc.client.IDfPersistentObject)}
	 * .
	 */
	@Test
	public void testCopyAttributeToObjectStringT() {
		Assert.fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link com.delta.cmsmf.cms.CmsObject#copyAttributeToObject(com.delta.cmsmf.cms.CmsAttribute, com.documentum.fc.client.IDfPersistentObject)}
	 * .
	 */
	@Test
	public void testCopyAttributeToObjectCmsAttributeT() {
		Assert.fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link com.delta.cmsmf.cms.CmsObject#setAttributeOnObject(java.lang.String, com.documentum.fc.common.IDfValue, com.documentum.fc.client.IDfPersistentObject)}
	 * .
	 */
	@Test
	public void testSetAttributeOnObjectStringIDfValueT() {
		Assert.fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link com.delta.cmsmf.cms.CmsObject#setAttributeOnObject(java.lang.String, java.util.Collection, com.documentum.fc.client.IDfPersistentObject)}
	 * .
	 */
	@Test
	public void testSetAttributeOnObjectStringCollectionOfIDfValueT() {
		Assert.fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link com.delta.cmsmf.cms.CmsObject#setAttributeOnObject(com.delta.cmsmf.cms.CmsAttribute, com.documentum.fc.common.IDfValue, com.documentum.fc.client.IDfPersistentObject)}
	 * .
	 */
	@Test
	public void testSetAttributeOnObjectCmsAttributeIDfValueT() {
		Assert.fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link com.delta.cmsmf.cms.CmsObject#setAttributeOnObject(com.delta.cmsmf.cms.CmsAttribute, java.util.Collection, com.documentum.fc.client.IDfPersistentObject)}
	 * .
	 */
	@Test
	public void testSetAttributeOnObjectCmsAttributeCollectionOfIDfValueT() {
		Assert.fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link com.delta.cmsmf.cms.CmsObject#clearAttributeFromObject(java.lang.String, com.documentum.fc.client.IDfPersistentObject)}
	 * .
	 */
	@Test
	public void testClearAttributeFromObjectStringT() {
		Assert.fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link com.delta.cmsmf.cms.CmsObject#clearAttributeFromObject(com.delta.cmsmf.cms.CmsAttribute, com.documentum.fc.client.IDfPersistentObject)}
	 * .
	 */
	@Test
	public void testClearAttributeFromObjectCmsAttributeT() {
		Assert.fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link com.delta.cmsmf.cms.CmsObject#clearAttributeFromObject(com.documentum.fc.common.IDfAttr, com.documentum.fc.client.IDfPersistentObject)}
	 * .
	 */
	@Test
	public void testClearAttributeFromObjectIDfAttrT() {
		Assert.fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link com.delta.cmsmf.cms.CmsObject#clearAttributeFromObject(java.lang.String, com.delta.cmsmf.cms.CmsDataType, boolean, com.documentum.fc.client.IDfPersistentObject)}
	 * .
	 */
	@Test
	public void testClearAttributeFromObjectStringCmsDataTypeBooleanT() {
		Assert.fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link com.delta.cmsmf.cms.CmsObject#updateModifyDate(com.documentum.fc.client.IDfPersistentObject)}
	 * .
	 */
	@Test
	public void testUpdateModifyDate() {
		Assert.fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link com.delta.cmsmf.cms.CmsObject#updateVStamp(int, com.documentum.fc.client.IDfPersistentObject)}
	 * .
	 */
	@Test
	public void testUpdateVStampIntT() {
		Assert.fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link com.delta.cmsmf.cms.CmsObject#updateVStamp(java.lang.String, int, com.documentum.fc.client.IDfPersistentObject)}
	 * .
	 */
	@Test
	public void testUpdateVStampStringIntT() {
		Assert.fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link com.delta.cmsmf.cms.CmsObject#closeQuietly(com.documentum.fc.client.IDfCollection)}.
	 */
	@Test
	public void testCloseQuietly() {
		Assert.fail("Not yet implemented");
	}

}
