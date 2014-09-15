/**
 *
 */

package com.delta.cmsmf.cms;

import java.util.Collection;

import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.utils.DfUtils;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfPermit;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfList;
import com.documentum.fc.common.IDfValue;

/**
 * @author Diego Rivera <diego.rivera@armedia.com>
 *
 */
public class CmsACL extends CmsObject<IDfACL> {

	private static final String USERS_WITH_DEFAULT_ACL = "usersWithDefaultACL";
	private static final String ACCESSORS = "accessors";
	private static final String ACCESSOR_IS_GROUP = "accessorIsGroup";
	private static final String EXTENDED_PERMISSIONS = "extendedPermissions";
	private static final String REGULAR_PERMISSIONS = "regularPermissions";

	private static boolean HANDLERS_READY = false;

	private static synchronized void initHandlers() {
		if (CmsACL.HANDLERS_READY) { return; }
		// These are the attributes that require special handling on import
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.ACL, CmsDataType.DF_STRING, CmsAttributes.OWNER_NAME,
			CmsAttributeHandlers.SESSION_CONFIG_USER_HANDLER);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.ACL, CmsDataType.DF_STRING, CmsAttributes.OBJECT_NAME,
			CmsAttributeHandlers.NO_IMPORT_HANDLER);

		// TODO: Handle special mappings
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.ACL, CmsDataType.DF_STRING,
			CmsAttributes.R_ACCESSOR_NAME, CmsAttributeHandlers.NO_IMPORT_HANDLER);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.ACL, CmsDataType.DF_STRING,
			CmsAttributes.R_ACCESSOR_PERMIT, CmsAttributeHandlers.NO_IMPORT_HANDLER);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.ACL, CmsDataType.DF_STRING, CmsAttributes.R_IS_GROUP,
			CmsAttributeHandlers.NO_IMPORT_HANDLER);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.ACL, CmsDataType.DF_STRING,
			CmsAttributes.R_ACCESSOR_XPERMIT, CmsAttributeHandlers.NO_IMPORT_HANDLER);

		CmsACL.HANDLERS_READY = true;
	}

	/**
	 * This DQL will find all users for which this ACL is marked as the default ACL, and thus all
	 * users for whom it must be restored later on.
	 */
	private static final String DQL_FIND_USERS_WITH_DEFAULT_ACL = "SELECT u.user_name FROM dm_user u, dm_acl a WHERE u.acl_domain = a.owner_name AND u.acl_name = a.object_name AND a.r_object_id = '%s'";

	public CmsACL() {
		super(IDfACL.class);
		CmsACL.initHandlers();
	}

	@Override
	protected String calculateLabel(IDfACL acl) throws DfException {
		return String.format("%s::%s", acl.getDomain(), acl.getObjectName());
	}

	@Override
	protected void getDataProperties(Collection<CmsProperty> properties, IDfACL acl) throws DfException {
		final String aclId = acl.getObjectId().getId();
		IDfCollection resultCol = DfUtils.executeQuery(acl.getSession(),
			String.format(CmsACL.DQL_FIND_USERS_WITH_DEFAULT_ACL, aclId), IDfQuery.DF_EXECREAD_QUERY);
		CmsProperty property = null;
		try {
			property = new CmsProperty(CmsACL.USERS_WITH_DEFAULT_ACL, CmsDataType.DF_STRING);
			while (resultCol.next()) {
				property.addValue(resultCol.getValueAt(0));
			}
			properties.add(property);
		} finally {
			DfUtils.closeQuietly(resultCol);
		}

		CmsProperty accessors = new CmsProperty(CmsACL.ACCESSORS, CmsDataType.DF_STRING, true);
		CmsProperty permissions = new CmsProperty(CmsACL.REGULAR_PERMISSIONS, CmsDataType.DF_INTEGER, true);
		CmsProperty extended = new CmsProperty(CmsACL.EXTENDED_PERMISSIONS, CmsDataType.DF_STRING, true);
		CmsProperty accessorIsGroup = new CmsProperty(CmsACL.ACCESSOR_IS_GROUP, CmsDataType.DF_BOOLEAN, true);
		final int count = acl.getAccessorCount();
		for (int i = 0; i < count; i++) {
			accessors.addValue(DfValueFactory.newStringValue(acl.getAccessorName(i)));
			permissions.addValue(DfValueFactory.newIntValue(acl.getAccessorPermit(i)));
			extended.addValue(DfValueFactory.newStringValue(acl.getAccessorXPermitNames(i)));
			accessorIsGroup.addValue(DfValueFactory.newBooleanValue(acl.isGroup(i)));
		}
		properties.add(accessors);
		properties.add(permissions);
		properties.add(extended);
		properties.add(accessorIsGroup);
	}

	@Override
	protected void doPersistDependents(IDfACL acl, CmsDependencyManager dependencyManager) throws DfException,
	CMSMFException {
		final int count = acl.getAccessorCount();
		final IDfSession session = acl.getSession();
		for (int i = 0; i < count; i++) {
			final String name = acl.getAccessorName(i);
			final boolean group = acl.isGroup(i);

			if (!group) {
				if (CmsMappingUtils.isSpecialUser(session, name)) {
					// User is mapped to a special user, so we shouldn't include it as a dependency
					// because it will be mapped on the target
					continue;
				}
			}

			if (CmsMappingUtils.SPECIAL_NAMES.contains(name)) {
				// This is a special name - non-existent per-se, but supported by the system
				// such as dm_owner, dm_group, dm_world
				continue;
			}

			final IDfPersistentObject obj = (group ? session.getGroup(name) : session.getUser(name));
			if (obj == null) { throw new CMSMFException(String.format(
				"Missing dependency for ACL [%s] - %s [%s] not found (as ACL accessor)", acl.getObjectName(),
				(group ? "group" : "user"), name)); }
			dependencyManager.persistRelatedObject(obj);
		}

		// Do the owner
		final String owner = acl.getDomain();
		if (CmsMappingUtils.isSpecialUser(session, owner)) {
			this.log.warn(String.format("Skipping export of special user [%s]", owner));
		} else {
			IDfUser user = session.getUser(acl.getDomain());
			if (user == null) { throw new CMSMFException(String.format(
				"Missing dependency for ACL [%s] - user [%s] not found (as ACL domain)", acl.getObjectName(), owner)); }
		}
	}

	@Override
	protected void finalizeConstruction(IDfACL acl, boolean newObject) throws DfException {
		if (newObject) {
			copyAttributeToObject(CmsAttributes.OWNER_NAME, acl);
			copyAttributeToObject(CmsAttributes.OBJECT_NAME, acl);
			acl.save();
		}
		CmsProperty usersWithDefaultACL = getProperty(CmsACL.USERS_WITH_DEFAULT_ACL);
		if (usersWithDefaultACL != null) {
			final IDfSession session = acl.getSession();
			for (IDfValue value : CmsMappingUtils.resolveSpecialUsers(acl, usersWithDefaultACL)) {

				// TODO: How do we decide if we should update the default ACL for this user? What if
				// the user's default ACL has been modified on the target CMS and we don't want to
				// clobber that?
				final IDfUser user = session.getUser(value.asString());
				if (user == null) {
					this.log.warn(String.format(
						"Failed to link ACL [%s.%s] to user [%s] as its default ACL - the user wasn't found",
						acl.getDomain(), acl.getObjectName(), value.asString()));
					continue;
				}

				// Ok...so we relate this thing back to its owner as its internal ACL
				user.setDefaultACLEx(acl.getDomain(), acl.getObjectName());
				user.save();
			}
		}

		// Clear any existing permissions
		final IDfList existingPermissions = acl.getPermissions();
		final int existingPermissionCount = existingPermissions.getCount();
		for (int i = 0; i < existingPermissionCount; i++) {
			acl.revokePermit(IDfPermit.class.cast(existingPermissions.get(i)));
		}

		// Now, apply the new permissions
		CmsProperty accessors = getProperty(CmsACL.ACCESSORS);
		CmsProperty permissions = getProperty(CmsACL.REGULAR_PERMISSIONS);
		CmsProperty extended = getProperty(CmsACL.EXTENDED_PERMISSIONS);
		CmsProperty accessorIsGroup = getProperty(CmsACL.ACCESSOR_IS_GROUP);
		final int accessorCount = accessors.getValueCount();
		IDfSession session = acl.getSession();
		for (int i = 0; i < accessorCount; i++) {
			String name = accessors.getValue(i).asString();
			int perm = permissions.getValue(i).asInteger();
			String xperm = extended.getValue(i).asString();
			final boolean exists;
			final String accessorType;

			if (!CmsMappingUtils.SPECIAL_NAMES.contains(name)) {
				if (accessorIsGroup.getValue(i).asBoolean()) {
					accessorType = "group";
					exists = (acl.getSession().getGroup(name) != null);
				} else {
					name = CmsMappingUtils.resolveSpecialUser(session, name);
					accessorType = "user";
					exists = (acl.getSession().getUser(name) != null);
				}
			} else {
				exists = true;
				accessorType = "[SPECIAL]";
			}

			if (!exists) {
				this.log.warn(String.format(
					"ACL [%s.%s] references the %s [%s] for permissions [%d/%s], but the %s wasn't found",
					acl.getDomain(), acl.getObjectName(), accessorType, name, perm, xperm, accessorType));
				continue;
			}

			// TODO: How to support copying over application permissions?
			// TODO: How to preserve permit types?
			acl.grant(name, perm, xperm);
		}
	}

	@Override
	protected IDfACL locateInCms(IDfSession session) throws DfException {
		final IDfValue ownerName = getAttribute(CmsAttributes.OWNER_NAME).getValue();
		final IDfValue objectName = getAttribute(CmsAttributes.OBJECT_NAME).getValue();
		return session.getACL(ownerName != null ? CmsMappingUtils.resolveSpecialUser(session, ownerName.asString())
			: null, objectName.asString());
	}
}