/**
 *
 */

package com.delta.cmsmf.datastore.cms;

import java.util.Collection;

import com.delta.cmsmf.constants.DctmAttrNameConstants;
import com.delta.cmsmf.datastore.DataAttribute;
import com.delta.cmsmf.datastore.DataProperty;
import com.delta.cmsmf.datastore.DataType;
import com.delta.cmsmf.datastore.DfValueFactory;
import com.delta.cmsmf.datastore.cms.CmsAttributeHandlers.AttributeHandler;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class CmsUser extends CmsObject<IDfUser> {

	private static boolean HANDLERS_READY = false;

	private static synchronized void initHandlers() {
		if (CmsUser.HANDLERS_READY) { return; }
		AttributeHandler handler = new AttributeHandler() {
			@Override
			public Collection<IDfValue> getImportableValues(IDfPersistentObject object, DataAttribute attribute)
				throws DfException {
				return null;
			}

			@Override
			public Collection<IDfValue> getExportableValues(IDfPersistentObject object, IDfAttr attr)
				throws DfException {
				return null;
			}

			@Override
			public boolean includeInImport(IDfPersistentObject object, DataAttribute attribute) throws DfException {
				return false;
			}

			@Override
			public boolean includeInExport(IDfPersistentObject object, IDfAttr attr) throws DfException {
				return true;
			}
		};
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.USER, DataType.DF_STRING,
			DctmAttrNameConstants.USER_NAME, handler);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.USER, DataType.DF_STRING,
			DctmAttrNameConstants.HOME_DOCBASE, handler);
		CmsUser.HANDLERS_READY = true;
	}

	private static final String PROP_INTERNAL_ACL = "doesUserHaveInternalACL";

	public CmsUser() {
		super(CmsObjectType.USER, IDfUser.class);
		CmsUser.initHandlers();
	}

	@Override
	protected void getDataProperties(Collection<DataProperty> properties, IDfUser user) throws DfException {
		IDfACL acl = user.getSession().getACL(user.getACLDomain(), user.getACLName());
		properties.add(new DataProperty(CmsUser.PROP_INTERNAL_ACL, DataType.DF_BOOLEAN, false, DfValueFactory
			.newBooleanValue(acl.isInternal())));
	}

	@Override
	protected IDfUser locateInCms(IDfSession session) throws DfException {
		IDfValue userName = getAttribute(DctmAttrNameConstants.USER_NAME).getSingleValue();
		return session.getUser(userName.asString());
	}
}