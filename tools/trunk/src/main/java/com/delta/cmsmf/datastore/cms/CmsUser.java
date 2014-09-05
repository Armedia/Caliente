/**
 *
 */

package com.delta.cmsmf.datastore.cms;

import com.armedia.commons.utilities.Tools;
import com.delta.cmsmf.constants.CMSMFAppConstants;
import com.delta.cmsmf.constants.DctmAttrNameConstants;
import com.delta.cmsmf.datastore.DataAttribute;
import com.delta.cmsmf.datastore.DataType;
import com.delta.cmsmf.datastore.DfValueFactory;
import com.delta.cmsmf.datastore.cms.CmsAttributeHandlers.AttributeHandler;
import com.delta.cmsmf.properties.CMSMFProperties;
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
		// These are the attributes that require special handling on import
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.USER, DataType.DF_STRING,
			DctmAttrNameConstants.USER_NAME, handler);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.USER, DataType.DF_STRING,
			DctmAttrNameConstants.USER_PASSWORD, handler);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.USER, DataType.DF_STRING,
			DctmAttrNameConstants.HOME_DOCBASE, handler);

		// We avoid storing these two because it'll be the ACL's job to link back
		// to the users for whom they're marked as default ACL. This is CRITICAL
		// to allow us to do a one-pass import without having to circle back
		// to resolve circular dependencies.
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

	@Override
	protected void applyCustomizations(IDfUser user) throws DfException {

		// First, set the username
		final IDfValue userName = getAttribute(DctmAttrNameConstants.USER_NAME).getSingleValue();
		user.setValue(DctmAttrNameConstants.USER_NAME, userName);

		// Next, set the password
		final IDfValue userSource = getAttribute(DctmAttrNameConstants.USER_SOURCE).getSingleValue();
		if (Tools.equals(userSource.asString(), CMSMFAppConstants.USER_SOURCE_INLINE_PASSWORD)) {
			// Default the password to the user's login name, if a specific value hasn't been
			// selected for global use
			final String inlinePasswordValue = CMSMFProperties.DEFAULT_USER_PASSWORD.getString(user.getUserName());
			user.setValue(DctmAttrNameConstants.USER_PASSWORD, DfValueFactory.newStringValue(inlinePasswordValue));
		}

		// Next, set the home docbase
		final IDfValue newHomeDocbase = getAttribute(DctmAttrNameConstants.HOME_DOCBASE).getSingleValue();
		final String docbase = newHomeDocbase.asString();
		final String existingDocbase = user.getHomeDocbase();
		if (!docbase.equals("") && !Tools.equals(docbase, existingDocbase)) {
			user.changeHomeDocbase(docbase, true);
		}
	}
}