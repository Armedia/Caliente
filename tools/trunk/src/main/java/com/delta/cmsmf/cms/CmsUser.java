/**
 *
 */

package com.delta.cmsmf.cms;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.text.StrTokenizer;

import com.armedia.commons.utilities.Tools;
import com.delta.cmsmf.cfg.Constant;
import com.delta.cmsmf.cfg.Setting;
import com.delta.cmsmf.cms.CmsAttributeHandlers.AttributeHandler;
import com.delta.cmsmf.exception.CMSMFException;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfTypedObject;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfValue;

/**
 * @author Diego Rivera <diego.rivera@armedia.com>
 *
 */
public class CmsUser extends CmsObject<IDfUser> {

	static final AttributeHandler USER_NAME_HANDLER = new AttributeHandler() {
		@Override
		public Collection<IDfValue> getExportableValues(IDfPersistentObject object, IDfAttr attr) throws DfException {
			return CmsAttributeHandlers.SESSION_CONFIG_USER_HANDLER.getExportableValues(object, attr);
		}

		@Override
		public Collection<IDfValue> getImportableValues(IDfPersistentObject object, CmsAttribute attribute)
			throws DfException {
			return CmsAttributeHandlers.SESSION_CONFIG_USER_HANDLER.getImportableValues(object, attribute);
		}

		@Override
		public boolean includeInImport(IDfPersistentObject object, CmsAttribute attribute) throws DfException {
			return false;
		}
	};

	private static boolean HANDLERS_READY = false;

	private static synchronized void initHandlers() {
		if (CmsUser.HANDLERS_READY) { return; }
		// These are the attributes that require special handling on import
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.USER, CmsDataType.DF_STRING,
			CmsAttributes.USER_PASSWORD, CmsAttributeHandlers.NO_IMPORT_HANDLER);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.USER, CmsDataType.DF_STRING,
			CmsAttributes.USER_LOGIN_DOMAIN, CmsAttributeHandlers.NO_IMPORT_HANDLER);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.USER, CmsDataType.DF_STRING,
			CmsAttributes.USER_LOGIN_NAME, CmsAttributeHandlers.NO_IMPORT_HANDLER);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.USER, CmsDataType.DF_STRING, CmsAttributes.HOME_DOCBASE,
			CmsAttributeHandlers.NO_IMPORT_HANDLER);

		// We avoid storing these because it'll be the job of other classes to link back
		// to the users to which they're related. This is CRITICAL to allow us to do a one-pass
		// import without having to circle back to resolve circular dependencies, or getting
		// ahead of ourselves in the object creation phase.

		// The default ACL will be linked back when the ACL's are imported.
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.USER, CmsDataType.DF_STRING, CmsAttributes.ACL_DOMAIN,
			CmsAttributeHandlers.NO_IMPORT_HANDLER);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.USER, CmsDataType.DF_STRING, CmsAttributes.ACL_NAME,
			CmsAttributeHandlers.NO_IMPORT_HANDLER);

		// The default group will be linked back when the groups are imported
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.USER, CmsDataType.DF_STRING,
			CmsAttributes.USER_GROUP_NAME, CmsAttributeHandlers.NO_IMPORT_HANDLER);

		// The default folder will be linked back when the folders are imported
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.USER, CmsDataType.DF_STRING,
			CmsAttributes.DEFAULT_FOLDER, CmsAttributeHandlers.NO_IMPORT_HANDLER);

		// This will help intercept user names that need to be mapped to "dynamic" names on the
		// target DB, taken from the session config
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.USER, CmsDataType.DF_STRING, CmsAttributes.USER_NAME,
			CmsUser.USER_NAME_HANDLER);

		CmsUser.HANDLERS_READY = true;
	}

	private static boolean SPECIAL_USERS_READY = false;
	private static Set<String> SPECIAL_USERS = Collections.emptySet();

	private static synchronized void initSpecialUsers() {
		if (CmsUser.SPECIAL_USERS_READY) { return; }
		String specialUsers = Setting.SPECIAL_USERS.getString();
		StrTokenizer strTokenizer = StrTokenizer.getCSVInstance(specialUsers);
		CmsUser.SPECIAL_USERS = Collections.unmodifiableSet(new HashSet<String>(strTokenizer.getTokenList()));
		CmsUser.SPECIAL_USERS_READY = true;
	}

	public static boolean isSpecialUser(String user) {
		CmsUser.initSpecialUsers();
		return CmsUser.SPECIAL_USERS.contains(user);
	}

	public CmsUser() {
		super(CmsObjectType.USER, IDfUser.class);
		CmsUser.initHandlers();
		CmsUser.initSpecialUsers();
	}

	@Override
	protected String calculateLabel(IDfUser user) throws DfException {
		return user.getUserName();
	}

	@Override
	protected void doPersistDependencies(IDfUser user, CmsDependencyManager manager) throws DfException, CMSMFException {
		final IDfSession session = user.getSession();
		final IDfPersistentObject[] deps = {
			session.getGroup(user.getUserGroupName()), session.getFolderByPath(user.getDefaultFolder())
		};
		for (IDfPersistentObject dep : deps) {
			if (dep == null) {
				continue;
			}
			manager.persistDependency(dep);
		}
		IDfACL acl = session.getACL(user.getACLDomain(), user.getACLName());
		if (acl != null) {
			manager.persistDependency(acl);
		}
	}

	@Override
	protected IDfUser locateInCms(IDfSession session) throws DfException {
		// If that search failed, go by username
		String userName = getAttribute(CmsAttributes.USER_NAME).getValue().asString();
		userName = CmsMappingUtils.resolveSpecialUser(session, userName);
		IDfUser ret = session.getUser(userName);
		return ret;
	}

	@Override
	protected boolean isValidForLoad(IDfUser user) throws DfException {
		if (CmsUser.isSpecialUser(user.getUserName())) { return false; }
		return super.isValidForLoad(user);
	}

	@Override
	protected boolean skipImport(IDfSession session) throws DfException {
		IDfValue userNameValue = getAttribute(CmsAttributes.USER_NAME).getValue();
		final String userName = userNameValue.asString();
		if (CmsUser.isSpecialUser(userName)) { return true; }
		return super.skipImport(session);
	}

	@Override
	protected void prepareForConstruction(IDfUser user, boolean newObject) throws DfException {

		CmsAttribute loginDomain = getAttribute(CmsAttributes.USER_LOGIN_DOMAIN);
		IDfTypedObject serverConfig = user.getSession().getServerConfig();
		String serverVersion = serverConfig.getString(CmsAttributes.R_SERVER_VERSION);
		CmsAttribute userSourceAtt = getAttribute(CmsAttributes.USER_SOURCE);
		String userSource = (userSourceAtt != null ? userSourceAtt.getValue().asString() : null);

		// NOTE for some reason, 6.5 sp2 with ldap requires that user_login_domain be set
		// workaround for [DM_USER_E_MUST_HAVE_LOGINDOMAIN] error
		// Only do this for Documentum 6.5-SP2
		if ((loginDomain == null) && serverVersion.startsWith("6.5") && "LDAP".equalsIgnoreCase(userSource)) {
			IDfAttr attr = user.getAttr(user.findAttrIndex(CmsAttributes.USER_LOGIN_DOMAIN));
			loginDomain = new CmsAttribute(attr, DfValueFactory.newStringValue(""));
			setAttribute(loginDomain);
		}
	}

	@Override
	protected void finalizeConstruction(IDfUser user, boolean newObject) throws DfException {
		// First, set the username - only do this for new objects!!
		if (newObject) {
			copyAttributeToObject(CmsAttributes.USER_NAME, user);
			final String userName = getAttribute(CmsAttributes.USER_NAME).getValue().asString();

			// Login name + domain
			copyAttributeToObject(CmsAttributes.USER_LOGIN_DOMAIN, user);
			copyAttributeToObject(CmsAttributes.USER_LOGIN_NAME, user);

			// Next, set the password
			CmsAttribute att = getAttribute(CmsAttributes.USER_SOURCE);
			final IDfValue userSource = att.getValue();
			if (Tools.equals(Constant.USER_SOURCE_INLINE_PASSWORD, userSource.asString())) {
				// Default the password to the user's login name, if a specific value hasn't been
				// selected for global use
				final String inlinePasswordValue = Setting.DEFAULT_USER_PASSWORD.getString(userName);
				setAttributeOnObject(CmsAttributes.USER_PASSWORD,
					Collections.singletonList(DfValueFactory.newStringValue(inlinePasswordValue)), user);
			}
		}

		// Next, set the home docbase
		final IDfValue newHomeDocbase = getAttribute(CmsAttributes.HOME_DOCBASE).getValue();
		final String docbase = newHomeDocbase.asString();
		final String existingDocbase = user.getHomeDocbase();
		if (!docbase.equals("") && !Tools.equals(docbase, existingDocbase)) {
			user.changeHomeDocbase(docbase, true);
		}
	}
}