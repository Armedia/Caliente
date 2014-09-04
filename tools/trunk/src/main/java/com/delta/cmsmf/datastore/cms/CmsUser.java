/**
 *
 */

package com.delta.cmsmf.datastore.cms;

import com.delta.cmsmf.constants.DctmAttrNameConstants;
import com.delta.cmsmf.datastore.DataAttribute;
import com.delta.cmsmf.datastore.DataType;
import com.delta.cmsmf.datastore.cms.CmsAttributeHandlers.AttributeHandler;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.common.DfException;
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
			public boolean includeInImport(IDfPersistentObject object, DataAttribute attribute) throws DfException {
				return false;
			}
		};
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.USER, DataType.DF_STRING,
			DctmAttrNameConstants.USER_NAME, handler);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.USER, DataType.DF_STRING,
			DctmAttrNameConstants.HOME_DOCBASE, handler);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.USER, DataType.DF_STRING,
			DctmAttrNameConstants.ACL_DOMAIN, handler);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.USER, DataType.DF_STRING,
			DctmAttrNameConstants.ACL_NAME, handler);
		CmsUser.HANDLERS_READY = true;
	}

	public CmsUser() {
		super(CmsObjectType.USER, IDfUser.class);
		CmsUser.initHandlers();
	}

	@Override
	protected IDfUser locateInCms(IDfSession session) throws DfException {
		IDfValue userName = getAttribute(DctmAttrNameConstants.USER_NAME).getSingleValue();
		return session.getUser(userName.asString());
	}
}