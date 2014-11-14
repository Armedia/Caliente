/**
 *
 */

package com.armedia.cmf.documentum.engine.exporter;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.text.StrTokenizer;

import com.armedia.cmf.documentum.engine.DctmAttributeHandlers;
import com.armedia.cmf.documentum.engine.DctmAttributeHandlers.AttributeHandler;
import com.armedia.cmf.documentum.engine.DctmAttributes;
import com.armedia.cmf.documentum.engine.DctmDataType;
import com.armedia.cmf.documentum.engine.DctmObjectType;
import com.armedia.cmf.engine.exporter.ExportContext;
import com.armedia.cmf.storage.StoredAttribute;
import com.armedia.cmf.storage.StoredObject;
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
public class DctmExportUser extends DctmExportAbstract<IDfUser> {

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
		if (DctmExportUser.HANDLERS_READY) { return; }
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
			DctmAttributes.USER_NAME, DctmExportUser.USER_NAME_HANDLER);

		DctmExportUser.HANDLERS_READY = true;
	}

	private static boolean SPECIAL_USERS_READY = false;
	private static Set<String> SPECIAL_USERS = Collections.emptySet();

	private static synchronized void initSpecialUsers() {
		if (DctmExportUser.SPECIAL_USERS_READY) { return; }
		String specialUsers = Setting.SPECIAL_USERS.getString();
		StrTokenizer strTokenizer = StrTokenizer.getCSVInstance(specialUsers);
		DctmExportUser.SPECIAL_USERS = Collections.unmodifiableSet(new HashSet<String>(strTokenizer.getTokenList()));
		DctmExportUser.SPECIAL_USERS_READY = true;
	}

	public static boolean isSpecialUser(String user) {
		DctmExportUser.initSpecialUsers();
		return DctmExportUser.SPECIAL_USERS.contains(user);
	}

	protected DctmExportUser(DctmExportEngine engine) {
		super(engine, DctmObjectType.USER);
		DctmExportUser.initHandlers();
		DctmExportUser.initSpecialUsers();
	}

	@Override
	protected String calculateLabel(IDfSession session, IDfUser user) throws DfException {
		return user.getUserName();
	}

	@Override
	protected Collection<IDfPersistentObject> findRequirements(IDfSession session, StoredObject<IDfValue> marshaled,
		IDfUser user, ExportContext<IDfSession, IDfPersistentObject, IDfValue> ctx) throws Exception {
		Collection<IDfPersistentObject> ret = super.findRequirements(session, marshaled, user, ctx);
		final IDfPersistentObject[] deps = {
			session.getGroup(user.getUserGroupName()), session.getFolderByPath(user.getDefaultFolder()),
			session.getACL(user.getACLDomain(), user.getACLName())
		};
		for (IDfPersistentObject dep : deps) {
			if (dep == null) {
				continue;
			}
			ret.add(dep);
		}
		return ret;
	}
}