/**
 *
 */

package com.delta.cmsmf.datastore.cms;

import java.util.Collections;

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
			DctmAttrNameConstants.USER_LOGIN_DOMAIN, handler);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.USER, DataType.DF_STRING,
			DctmAttrNameConstants.USER_LOGIN_NAME, handler);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.USER, DataType.DF_STRING,
			DctmAttrNameConstants.HOME_DOCBASE, handler);

		// We avoid storing these because it'll be the job of other classes to link back
		// to the users to which they're related. This is CRITICAL to allow us to do a one-pass
		// import without having to circle back to resolve circular dependencies, or getting
		// ahead of ourselves in the object creation phase.
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.USER, DataType.DF_STRING,
			DctmAttrNameConstants.ACL_DOMAIN, handler);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.USER, DataType.DF_STRING,
			DctmAttrNameConstants.ACL_NAME, handler);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.USER, DataType.DF_STRING,
			DctmAttrNameConstants.DEFAULT_FOLDER, handler);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.USER, DataType.DF_STRING,
			DctmAttrNameConstants.USER_GROUP_NAME, handler);

		CmsUser.HANDLERS_READY = true;
	}

	public CmsUser() {
		super(CmsObjectType.USER, IDfUser.class);
		CmsUser.initHandlers();
	}

	@Override
	protected IDfUser locateInCms(IDfSession session) throws DfException {
		// If that search failed, go by username
		IDfValue userName = getAttribute(DctmAttrNameConstants.USER_NAME).getValue();
		IDfUser ret = session.getUser(userName.asString());
		if (ret != null) { return ret; }

		DataAttribute loginName = getAttribute(DctmAttrNameConstants.USER_LOGIN_NAME);
		DataAttribute loginDomain = getAttribute(DctmAttrNameConstants.USER_LOGIN_DOMAIN);
		return session.getUserByLoginName(loginName.getValue().asString(), loginDomain != null ? loginDomain.getValue()
			.asString() : null);
	}

	@Override
	protected boolean skipImport(IDfSession session) throws DfException {
		IDfValue userNameValue = getAttribute(DctmAttrNameConstants.USER_NAME).getValue();
		final String userName = userNameValue.asString();
		if (Tools.equals("dmadmin", userName) || userName.startsWith("dm_")) { return true; }
		return super.skipImport(session);
	}

	@Override
	protected void applyPreCustomizations(IDfUser user, boolean newObject) throws DfException {
		// NOTE for some reason, 6.5 sp2 with ldap requires that user_login_domain be set
		// workaround for [DM_USER_E_MUST_HAVE_LOGINDOMAIN] error
		DataAttribute attribute = getAttribute(DctmAttrNameConstants.USER_LOGIN_DOMAIN);
		if (attribute == null) {
			int idx = user.findAttrIndex(DctmAttrNameConstants.USER_LOGIN_DOMAIN);
			IDfAttr attr = user.getAttr(idx);
			attribute = new DataAttribute(user, attr, DfValueFactory.newStringValue(""));
			setAttribute(attribute);
		}
	}

	@Override
	protected void applyPostCustomizations(IDfUser user, boolean newObject) throws DfException {
		// First, set the username - only do this for new objects!!
		if (newObject) {
			copyAttributeToObject(DctmAttrNameConstants.USER_NAME, user);

			// Login name + domain
			copyAttributeToObject(DctmAttrNameConstants.USER_LOGIN_DOMAIN, user);
			copyAttributeToObject(DctmAttrNameConstants.USER_LOGIN_NAME, user);

			// Next, set the password
			DataAttribute att = getAttribute(DctmAttrNameConstants.USER_SOURCE);
			final IDfValue userSource = att.getValue();
			if (Tools.equals(userSource.asString(), CMSMFAppConstants.USER_SOURCE_INLINE_PASSWORD)) {
				// Default the password to the user's login name, if a specific value hasn't been
				// selected for global use
				final String inlinePasswordValue = CMSMFProperties.DEFAULT_USER_PASSWORD.getString(user.getUserName());
				setAttributeOnObject(DctmAttrNameConstants.USER_PASSWORD,
					Collections.singletonList(DfValueFactory.newStringValue(inlinePasswordValue)), user);
			}
		}

		// Next, set the home docbase
		final IDfValue newHomeDocbase = getAttribute(DctmAttrNameConstants.HOME_DOCBASE).getValue();
		final String docbase = newHomeDocbase.asString();
		final String existingDocbase = user.getHomeDocbase();
		if (!docbase.equals("") && !Tools.equals(docbase, existingDocbase)) {
			user.changeHomeDocbase(docbase, true);
		}
	}
}