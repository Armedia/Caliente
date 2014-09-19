/**
 *
 */

package com.delta.cmsmf.cms;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.armedia.commons.utilities.Tools;
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
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class CmsACL extends CmsObject<IDfACL> {

	private static class Permit implements Comparable<Permit> {
		private final String name;
		private final int permit;
		private final int xpermit;
		private final String appPermit;
		private final boolean group;
		private final int permitType;

		private Permit(IDfACL acl, int pos) throws DfException {
			this.name = acl.getAccessorName(pos);
			this.permit = acl.getAccessorPermit(pos);
			this.xpermit = acl.getAccessorXPermit(pos);
			this.appPermit = acl.getAccessorApplicationPermit(pos);
			this.group = acl.isGroup(pos);
			this.permitType = acl.getAccessorPermitType(pos);
		}

		private Permit(CmsACL acl, int pos) {
			CmsAttribute att = null;
			att = acl.getAttribute(CmsAttributes.R_ACCESSOR_NAME);
			this.name = att.getValue(pos).asString();
			att = acl.getAttribute(CmsAttributes.R_ACCESSOR_PERMIT);
			this.permit = att.getValue(pos).asInteger();
			att = acl.getAttribute(CmsAttributes.R_ACCESSOR_XPERMIT);
			this.xpermit = att.getValue(pos).asInteger();
			att = acl.getAttribute(CmsAttributes.R_APPLICATION_PERMIT);
			this.appPermit = att.getValue(pos).asString();
			att = acl.getAttribute(CmsAttributes.R_IS_GROUP);
			this.group = att.getValue(pos).asBoolean();
			att = acl.getAttribute(CmsAttributes.R_PERMIT_TYPE);
			this.permitType = att.getValue(pos).asInteger();
		}

		@Override
		public int hashCode() {
			return Tools.hashTool(this, null, this.name, this.permit, this.xpermit, this.appPermit, this.group,
				this.permitType);
		}

		@Override
		public boolean equals(Object o) {
			if (!Tools.baseEquals(this, o)) { return false; }
			Permit other = Permit.class.cast(o);
			if (!Tools.equals(this.name, other.name)) { return false; }
			if (this.permit != other.permit) { return false; }
			if (this.xpermit != other.xpermit) { return false; }
			if (!Tools.equals(this.appPermit, other.appPermit)) { return false; }
			if (this.group != other.group) { return false; }
			if (this.permitType != other.permitType) { return false; }
			return true;
		}

		// This isn't really required, but it will help if we need to debug
		@Override
		public int compareTo(Permit o) {
			if (o == null) { return 1; }
			if (this.permitType != o.permitType) { return this.permitType - o.permitType; }
			if (!this.group && o.group) { return -1; }
			if (this.group && !o.group) { return 1; }
			return Tools.compare(this.name, o.name);
		}

		@Override
		public String toString() {
			return String.format("Permit [name=%s, permit=%s, xpermit=%s, appPermit=%s, group=%s, permitType=%s]",
				this.name, this.permit, this.xpermit, this.appPermit, this.group, this.permitType);
		}
	}

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
	protected IDfACL locateInCms(CmsTransferContext ctx) throws DfException {
		final IDfValue ownerName = getAttribute(CmsAttributes.OWNER_NAME).getValue();
		final IDfValue objectName = getAttribute(CmsAttributes.OBJECT_NAME).getValue();
		final IDfSession session = ctx.getSession();
		return session.getACL(ownerName != null ? CmsMappingUtils.resolveSpecialUser(session, ownerName.asString())
			: null, objectName.asString());
	}

	@Override
	protected boolean isSameObject(IDfACL acl) throws DfException {
		// TODO: Fix this comparison for ACL-specific lookups
		// ACL's don't have a modification date, so we actually have to compare them
		// if this is a potential match, then it was found using domain and object name,
		// so we don't need to check those two, but we do need to check the permissions
		// granted to see if they match. In particular, the target ACL may be a superset
		// of the one we're trying to bring in on top of it, but never a subset.
		CmsAttribute att = null;

		att = getAttribute(CmsAttributes.ACL_CLASS);
		if (att.getValue().asInteger() != acl.getACLClass()) { return false; }

		att = getAttribute(CmsAttributes.GLOBALLY_MANAGED);
		if (att.getValue().asBoolean() != acl.isGloballyManaged()) { return false; }

		att = getAttribute(CmsAttributes.I_HAS_ACCESS_RESTRICTIONS);
		if (att.getValue().asBoolean() != acl.getBoolean(att.getName())) { return false; }

		att = getAttribute(CmsAttributes.I_HAS_REQUIRED_GROUPS);
		if (att.getValue().asBoolean() != acl.getBoolean(att.getName())) { return false; }

		att = getAttribute(CmsAttributes.I_HAS_REQUIRED_GROUP_SET);
		if (att.getValue().asBoolean() != acl.getBoolean(att.getName())) { return false; }

		// Now, do the accessors
		att = getAttribute(CmsAttributes.R_ACCESSOR_NAME);
		final int accessorCount = acl.getAccessorCount();
		if (accessorCount < att.getValueCount()) {
			// If the target has the same or more elements as the ones
			// we're bringin in, then we have to compare the sets
			return false;
		}

		Set<Permit> existing = new HashSet<Permit>();
		Set<Permit> incoming = new HashSet<Permit>();
		for (int i = 0; i < accessorCount; i++) {
			existing.add(new Permit(acl, i));
			incoming.add(new Permit(this, i));
		}

		// The ACLs are considered the same only if the existing set contains
		// every element in the incoming set, and possibly more. That means
		// that we subtract the existing permissions from incoming, and if
		// the resulting set is empty, then this is the same ACL and doesn't need
		// to be updated.
		incoming.removeAll(existing);

		boolean ret = incoming.isEmpty();
		if (!ret) {
			incoming.size();
		}
		return ret;
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
	protected void doPersistDependents(IDfACL acl, CmsTransferContext ctx, CmsDependencyManager dependencyManager)
		throws DfException, CMSMFException {
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
	protected void finalizeConstruction(IDfACL acl, boolean newObject, CmsTransferContext context) throws DfException {
		if (newObject) {
			String user = getAttribute(CmsAttributes.OWNER_NAME).getValue().asString();
			user = CmsMappingUtils.resolveSpecialUser(acl.getSession(), user);
			acl.setDomain(user);
			acl.setObjectName(getAttribute(CmsAttributes.OBJECT_NAME).getValue().asString());
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
				// Update the system attributes, if we can
				try {
					updateSystemAttributes(user, context);
				} catch (CMSMFException e) {
					this.log
						.warn(
							String
								.format(
									"Failed to update the system attributes for user [%s] after assigning ACL [%s] as their default ACL",
									user.getUserName(), getLabel()), e);
				}
			}
		}

		// Clear any existing permissions
		final IDfList existingPermissions = acl.getPermissions();
		final int existingPermissionCount = existingPermissions.getCount();
		for (int i = 0; i < existingPermissionCount; i++) {
			IDfPermit permit = IDfPermit.class.cast(existingPermissions.get(i));
			if (this.log.isDebugEnabled()) {
				this.log.debug(String.format("PERMIT REVOKED on [%s]: [%s|%d|%d (%s)]", getLabel(),
					permit.getAccessorName(), permit.getPermitType(), permit.getPermitValueInt(),
					permit.getPermitValueString()));
			}
			acl.revokePermit(permit);
		}

		// Now, apply the new permissions
		CmsProperty accessors = getProperty(CmsACL.ACCESSORS);
		if (this.log.isTraceEnabled()) {
			this.log.trace(String.format("[%s]: %s", getLabel(), accessors));
		}
		CmsProperty permissions = getProperty(CmsACL.REGULAR_PERMISSIONS);
		if (this.log.isTraceEnabled()) {
			this.log.trace(String.format("[%s]: %s", getLabel(), permissions));
		}
		CmsProperty extended = getProperty(CmsACL.EXTENDED_PERMISSIONS);
		if (this.log.isTraceEnabled()) {
			this.log.trace(String.format("[%s]: %s", getLabel(), extended));
		}
		CmsProperty accessorIsGroup = getProperty(CmsACL.ACCESSOR_IS_GROUP);
		if (this.log.isTraceEnabled()) {
			this.log.trace(String.format("[%s]: %s", getLabel(), accessorIsGroup));
		}
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
			if (this.log.isDebugEnabled()) {
				this.log.debug(String.format("PERMIT GRANTED on [%s]: [%s|%d|%s]", getLabel(), name, perm, xperm));
			}
			acl.grant(name, perm, xperm);
			if (StringUtils.isBlank(xperm)) {
				// Default permits will be added - this is not acceptable!! Revoke them manually
				acl.revoke(name, IDfACL.DF_XPERMIT_EXECUTE_PROC_STR);
				acl.revoke(name, IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR);
			}
		}
	}
}