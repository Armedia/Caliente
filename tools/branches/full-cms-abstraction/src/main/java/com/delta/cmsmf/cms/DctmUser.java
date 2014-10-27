/**
 *
 */

package com.delta.cmsmf.cms;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.text.StrTokenizer;

import com.armedia.cmf.storage.StoredAttribute;
import com.armedia.commons.utilities.Tools;
import com.delta.cmsmf.cfg.Constant;
import com.delta.cmsmf.cfg.Setting;
import com.delta.cmsmf.cms.DctmAttributeHandlers.AttributeHandler;
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
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class DctmUser extends DctmPersistentObject<IDfUser> {

	static final AttributeHandler USER_NAME_HANDLER = new AttributeHandler() {
		@Override
		public Collection<IDfValue> getExportableValues(IDfPersistentObject object, IDfAttr attr) throws DfException {
			return DctmAttributeHandlers.SESSION_CONFIG_USER_HANDLER.getExportableValues(object, attr);
		}

		@Override
		public Collection<IDfValue> getImportableValues(IDfPersistentObject object,
			StoredAttribute<IDfValue> attribute) throws DfException {
			return DctmAttributeHandlers.SESSION_CONFIG_USER_HANDLER.getImportableValues(object, attribute);
		}

		@Override
		public boolean includeInImport(IDfPersistentObject object, StoredAttribute<IDfValue> attribute)
			throws DfException {
			return false;
		}
	};

	private static boolean HANDLERS_READY = false;

	private static synchronized void initHandlers() {
		if (DctmUser.HANDLERS_READY) { return; }
		// These are the attributes that require special handling on import
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.USER, DctmDataType.DF_STRING,
			DctmAttributes.USER_PASSWORD, DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.USER, DctmDataType.DF_STRING,
			DctmAttributes.USER_LOGIN_DOMAIN, DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.USER, DctmDataType.DF_STRING,
			DctmAttributes.USER_LOGIN_NAME, DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.USER, DctmDataType.DF_STRING,
			DctmAttributes.HOME_DOCBASE, DctmAttributeHandlers.NO_IMPORT_HANDLER);

		// We avoid storing these because it'll be the job of other classes to link back
		// to the users to which they're related. This is CRITICAL to allow us to do a one-pass
		// import without having to circle back to resolve circular dependencies, or getting
		// ahead of ourselves in the object creation phase.

		// The default ACL will be linked back when the ACL's are imported.
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.USER, DctmDataType.DF_STRING, DctmAttributes.ACL_DOMAIN,
			DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.USER, DctmDataType.DF_STRING, DctmAttributes.ACL_NAME,
			DctmAttributeHandlers.NO_IMPORT_HANDLER);

		// The default group will be linked back when the groups are imported
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.USER, DctmDataType.DF_STRING,
			DctmAttributes.USER_GROUP_NAME, DctmAttributeHandlers.NO_IMPORT_HANDLER);

		// The default folder will be linked back when the folders are imported
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.USER, DctmDataType.DF_STRING,
			DctmAttributes.DEFAULT_FOLDER, DctmAttributeHandlers.NO_IMPORT_HANDLER);

		// This will help intercept user names that need to be mapped to "dynamic" names on the
		// target DB, taken from the session config
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.USER, DctmDataType.DF_STRING, DctmAttributes.USER_NAME,
			DctmUser.USER_NAME_HANDLER);

		DctmUser.HANDLERS_READY = true;
	}

	private static boolean SPECIAL_USERS_READY = false;
	private static Set<String> SPECIAL_USERS = Collections.emptySet();

	private static synchronized void initSpecialUsers() {
		if (DctmUser.SPECIAL_USERS_READY) { return; }
		String specialUsers = Setting.SPECIAL_USERS.getString();
		StrTokenizer strTokenizer = StrTokenizer.getCSVInstance(specialUsers);
		DctmUser.SPECIAL_USERS = Collections.unmodifiableSet(new HashSet<String>(strTokenizer.getTokenList()));
		DctmUser.SPECIAL_USERS_READY = true;
	}

	public static boolean isSpecialUser(String user) {
		DctmUser.initSpecialUsers();
		return DctmUser.SPECIAL_USERS.contains(user);
	}

	public DctmUser() {
		super(IDfUser.class);
		DctmUser.initHandlers();
		DctmUser.initSpecialUsers();
	}

	@Override
	protected String calculateLabel(IDfUser user) throws DfException {
		return user.getUserName();
	}

	@Override
	protected void doPersistDependents(IDfUser user, DctmTransferContext ctx, DctmDependencyManager manager)
		throws DfException, CMSMFException {
		final IDfSession session = user.getSession();
		final IDfPersistentObject[] deps = {
			session.getGroup(user.getUserGroupName()), session.getFolderByPath(user.getDefaultFolder())
		};
		for (IDfPersistentObject dep : deps) {
			if (dep == null) {
				continue;
			}
			manager.persistRelatedObject(dep);
		}
		IDfACL acl = session.getACL(user.getACLDomain(), user.getACLName());
		if (acl != null) {
			manager.persistRelatedObject(acl);
		}
	}

	@Override
	protected IDfUser locateInCms(DctmTransferContext ctx) throws DfException {
		// If that search failed, go by username
		final IDfSession session = ctx.getSession();
		String userName = getAttribute(DctmAttributes.USER_NAME).getValue().asString();
		userName = DctmMappingUtils.resolveMappableUser(session, userName);
		IDfUser ret = session.getUser(userName);
		return ret;
	}

	@Override
	protected boolean isValidForLoad(IDfUser user) throws DfException {
		if (DctmUser.isSpecialUser(user.getUserName())) { return false; }
		return super.isValidForLoad(user);
	}

	@Override
	protected boolean skipImport(DctmTransferContext ctx) throws DfException {
		IDfValue userNameValue = getAttribute(DctmAttributes.USER_NAME).getValue();
		final String userName = userNameValue.asString();
		if (DctmUser.isSpecialUser(userName)) { return true; }
		return super.skipImport(ctx);
	}

	@Override
	protected void prepareForConstruction(IDfUser user, boolean newObject, DctmTransferContext context)
		throws DfException {

		StoredAttribute<IDfValue> loginDomain = getAttribute(DctmAttributes.USER_LOGIN_DOMAIN);
		IDfTypedObject serverConfig = user.getSession().getServerConfig();
		String serverVersion = serverConfig.getString(DctmAttributes.R_SERVER_VERSION);
		StoredAttribute<IDfValue> userSourceAtt = getAttribute(DctmAttributes.USER_SOURCE);
		String userSource = (userSourceAtt != null ? userSourceAtt.getValue().asString() : null);

		// NOTE for some reason, 6.5 sp2 with ldap requires that user_login_domain be set
		// workaround for [DM_USER_E_MUST_HAVE_LOGINDOMAIN] error
		// Only do this for Documentum 6.5-SP2
		if ((loginDomain == null) && serverVersion.startsWith("6.5") && "LDAP".equalsIgnoreCase(userSource)) {
			IDfAttr attr = user.getAttr(user.findAttrIndex(DctmAttributes.USER_LOGIN_DOMAIN));
			loginDomain = newStoredAttribute(attr, DfValueFactory.newStringValue(""));
			setAttribute(loginDomain);
		}
	}

	@Override
	protected void finalizeConstruction(IDfUser user, boolean newObject, DctmTransferContext context) throws DfException {
		// First, set the username - only do this for new objects!!
		if (newObject) {
			copyAttributeToObject(DctmAttributes.USER_NAME, user);
			final String userName = getAttribute(DctmAttributes.USER_NAME).getValue().asString();

			// Login name + domain
			copyAttributeToObject(DctmAttributes.USER_LOGIN_DOMAIN, user);
			copyAttributeToObject(DctmAttributes.USER_LOGIN_NAME, user);

			// Next, set the password
			StoredAttribute<IDfValue> att = getAttribute(DctmAttributes.USER_SOURCE);
			final IDfValue userSource = att.getValue();
			if (Tools.equals(Constant.USER_SOURCE_INLINE_PASSWORD, userSource.asString())) {
				// Default the password to the user's login name, if a specific value hasn't been
				// selected for global use
				final String inlinePasswordValue = Setting.DEFAULT_USER_PASSWORD.getString(userName);
				setAttributeOnObject(DctmAttributes.USER_PASSWORD,
					Collections.singletonList(DfValueFactory.newStringValue(inlinePasswordValue)), user);
			}
		}

		// Next, set the home docbase
		// TODO: Disabled, for now...for some reason the dm_job was failing on SPDMS_SI
		/*
		final IDfValue newHomeDocbase = getAttribute(CmsAttributes.HOME_DOCBASE).getValue();
		final String docbase = newHomeDocbase.asString();
		final String existingDocbase = user.getHomeDocbase();
		if (!docbase.equals("") && !Tools.equals(docbase, existingDocbase)) {
			user.changeHomeDocbase(docbase, true);
		}
		 */
	}
}