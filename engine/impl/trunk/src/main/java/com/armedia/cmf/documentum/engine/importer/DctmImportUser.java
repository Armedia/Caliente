/**
 *
 */

package com.armedia.cmf.documentum.engine.importer;

import java.util.Collection;
import java.util.Collections;

import com.armedia.cmf.documentum.engine.DctmAttributeHandlers;
import com.armedia.cmf.documentum.engine.DctmAttributeHandlers.AttributeHandler;
import com.armedia.cmf.documentum.engine.DctmAttributes;
import com.armedia.cmf.documentum.engine.DctmConstant;
import com.armedia.cmf.documentum.engine.DctmDataType;
import com.armedia.cmf.documentum.engine.DctmMappingUtils;
import com.armedia.cmf.documentum.engine.DctmObjectType;
import com.armedia.cmf.documentum.engine.DfValueFactory;
import com.armedia.cmf.documentum.engine.common.Setting;
import com.armedia.cmf.storage.StoredAttribute;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.commons.utilities.Tools;
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
public class DctmImportUser extends DctmImportDelegate<IDfUser> {

	static final AttributeHandler USER_NAME_HANDLER = new AttributeHandler() {
		@Override
		public Collection<IDfValue> getExportableValues(IDfPersistentObject object, IDfAttr attr) throws DfException {
			return DctmAttributeHandlers.SESSION_CONFIG_USER_HANDLER.getExportableValues(object, attr);
		}

		@Override
		public Collection<IDfValue> getImportableValues(IDfPersistentObject object, StoredAttribute<IDfValue> attribute)
			throws DfException {
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
		if (DctmImportUser.HANDLERS_READY) { return; }
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
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.USER, DctmDataType.DF_STRING,
			DctmAttributes.ACL_DOMAIN, DctmAttributeHandlers.NO_IMPORT_HANDLER);
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
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.USER, DctmDataType.DF_STRING,
			DctmAttributes.USER_NAME, DctmImportUser.USER_NAME_HANDLER);

		DctmImportUser.HANDLERS_READY = true;
	}

	public DctmImportUser(DctmImportEngine engine, StoredObject<IDfValue> storedObject) {
		super(engine, DctmObjectType.USER, storedObject);
		DctmImportUser.initHandlers();
	}

	@Override
	protected String calculateLabel(IDfUser user) throws DfException {
		return user.getUserName();
	}

	@Override
	protected IDfUser locateInCms(DctmImportContext ctx) throws DfException {
		// If that search failed, go by username
		final IDfSession session = ctx.getSession();
		String userName = this.storedObject.getAttribute(DctmAttributes.USER_NAME).getValue().asString();
		userName = DctmMappingUtils.resolveMappableUser(session, userName);
		IDfUser ret = session.getUser(userName);
		return ret;
	}

	@Override
	protected boolean isValidForLoad(DctmImportContext ctx, IDfUser user) throws DfException {
		if (ctx.isSpecialUser(user.getUserName())) { return false; }
		return super.isValidForLoad(ctx, user);
	}

	@Override
	protected boolean skipImport(DctmImportContext ctx) throws DfException {
		IDfValue userNameValue = this.storedObject.getAttribute(DctmAttributes.USER_NAME).getValue();
		final String userName = userNameValue.asString();
		if (ctx.isSpecialUser(userName)) { return true; }
		return super.skipImport(ctx);
	}

	@Override
	protected void prepareForConstruction(IDfUser user, boolean newObject, DctmImportContext context)
		throws DfException {

		StoredAttribute<IDfValue> loginDomain = this.storedObject.getAttribute(DctmAttributes.USER_LOGIN_DOMAIN);
		IDfTypedObject serverConfig = user.getSession().getServerConfig();
		String serverVersion = serverConfig.getString(DctmAttributes.R_SERVER_VERSION);
		StoredAttribute<IDfValue> userSourceAtt = this.storedObject.getAttribute(DctmAttributes.USER_SOURCE);
		String userSource = (userSourceAtt != null ? userSourceAtt.getValue().asString() : null);

		// NOTE for some reason, 6.5 sp2 with ldap requires that user_login_domain be set
		// workaround for [DM_USER_E_MUST_HAVE_LOGINDOMAIN] error
		// Only do this for Documentum 6.5-SP2
		if ((loginDomain == null) && serverVersion.startsWith("6.5") && "LDAP".equalsIgnoreCase(userSource)) {
			IDfAttr attr = user.getAttr(user.findAttrIndex(DctmAttributes.USER_LOGIN_DOMAIN));
			loginDomain = newStoredAttribute(attr, DfValueFactory.newStringValue(""));
			this.storedObject.setAttribute(loginDomain);
		}
	}

	@Override
	protected void finalizeConstruction(IDfUser user, boolean newObject, DctmImportContext ctx) throws DfException {
		// First, set the username - only do this for new objects!!
		if (newObject) {
			copyAttributeToObject(DctmAttributes.USER_NAME, user);
			final String userName = this.storedObject.getAttribute(DctmAttributes.USER_NAME).getValue().asString();

			// Login name + domain
			copyAttributeToObject(DctmAttributes.USER_LOGIN_DOMAIN, user);
			copyAttributeToObject(DctmAttributes.USER_LOGIN_NAME, user);

			// Next, set the password
			StoredAttribute<IDfValue> att = this.storedObject.getAttribute(DctmAttributes.USER_SOURCE);
			final IDfValue userSource = att.getValue();
			if (Tools.equals(DctmConstant.USER_SOURCE_INLINE_PASSWORD, userSource.asString())) {
				// Default the password to the user's login name, if a specific value hasn't been
				// selected for global use
				final String inlinePasswordValue = ctx.getSettings().getString(
					Setting.DEFAULT_USER_PASSWORD.getLabel(), userName);
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