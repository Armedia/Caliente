/**
 *
 */

package com.delta.cmsmf.cms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.armedia.commons.utilities.Tools;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.utils.DfUtils;
import com.documentum.fc.client.DfPermit;
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
		private final int type;
		private final String value;

		private Permit(IDfSession session, IDfPermit permit) throws DfException {
			this.name = permit.getAccessorName();
			this.type = permit.getPermitType();
			this.value = permit.getPermitValueString();
		}

		private Permit(IDfSession session, CmsACL acl, int pos) throws DfException {
			CmsProperty prop = null;
			prop = acl.getProperty(CmsACL.ACCESSORS);
			this.name = CmsMappingUtils.resolveMappableUser(session, prop.getValue(pos).asString());
			prop = acl.getProperty(CmsACL.PERMIT_TYPE);
			this.type = prop.getValue(pos).asInteger();
			prop = acl.getProperty(CmsACL.PERMIT_VALUE);
			this.value = prop.getValue(pos).asString();
		}

		@Override
		public int hashCode() {
			return Tools.hashTool(this, null, this.name, this.type, this.value);
		}

		@Override
		public boolean equals(Object o) {
			if (!Tools.baseEquals(this, o)) { return false; }
			Permit other = Permit.class.cast(o);
			return compareTo(other) == 0;
		}

		// This isn't really required, but it will help if we need to debug
		@Override
		public int compareTo(Permit o) {
			if (o == null) { return 1; }
			int nameComp = Tools.compare(this.name, o.name);
			if (nameComp != 0) { return nameComp; }
			if (this.type != o.type) { return this.type - o.type; }
			return Tools.compare(this.value, o.value);
		}

		@Override
		public String toString() {
			return String.format("Permit [name=%s, type=%s, value=%s]", this.name, this.type, this.value);
		}
	}

	private static final String USERS_WITH_DEFAULT_ACL = "usersWithDefaultACL";
	private static final String ACCESSORS = "accessors";
	private static final String PERMIT_TYPE = "permitType";
	private static final String PERMIT_VALUE = "permitValue";

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
		return session.getACL(ownerName != null ? CmsMappingUtils.resolveMappableUser(session, ownerName.asString())
			: null, objectName.asString());
	}

	@Override
	protected boolean isSameObject(IDfACL acl) throws DfException {
		// ACL's don't have a modification date, so we actually have to compare them.
		// If this is a potential match, then it was found using domain and object name,
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

		// Now, count the permits
		IDfList existingPermits = acl.getPermissions();
		CmsProperty prop = getProperty(CmsACL.ACCESSORS);
		if (existingPermits.getCount() != prop.getValueCount()) {
			// The permit counts have to be identical...
			return false;
		}

		Set<Permit> existing = new HashSet<Permit>();
		Set<Permit> incoming = new HashSet<Permit>();
		IDfSession session = acl.getSession();
		final int permitCount = existingPermits.getCount();
		for (int i = 0; i < permitCount; i++) {
			existing.add(new Permit(session, IDfPermit.class.cast(existingPermits.get(i))));
			incoming.add(new Permit(session, this, i));
		}

		// The ACLs are considered the same only if the sets are identical
		return incoming.equals(existing);
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
		CmsProperty permitTypes = new CmsProperty(CmsACL.PERMIT_TYPE, CmsDataType.DF_INTEGER, true);
		CmsProperty permitValues = new CmsProperty(CmsACL.PERMIT_VALUE, CmsDataType.DF_STRING, true);
		IDfList permits = acl.getPermissions();
		final int permitCount = permits.getCount();
		final IDfSession session = acl.getSession();
		Set<String> missingAccessors = new HashSet<String>();
		for (int i = 0; i < permitCount; i++) {
			IDfPermit p = IDfPermit.class.cast(permits.get(i));
			// First, validate the accessor
			final String accessor = p.getAccessorName();
			final boolean group;
			switch (p.getPermitType()) {
				case IDfPermit.DF_REQUIRED_GROUP:
				case IDfPermit.DF_REQUIRED_GROUP_SET:
					group = true;
					break;

				default:
					group = false;
					break;
			}

			IDfPersistentObject o = (group ? session.getGroup(accessor) : session.getUser(accessor));
			if (o == null) {
				// Accessor not there, skip it...
				if (!missingAccessors.contains(accessor)) {
					this.log.warn(String.format(
						"Missing dependency for ACL [%s] - %s [%s] not found (as ACL accessor)", getLabel(),
						(group ? "group" : "user"), accessor));
					missingAccessors.add(accessor);
				}
				continue;
			}

			accessors.addValue(DfValueFactory.newStringValue(CmsMappingUtils.substituteMappableUsers(acl, accessor)));
			permitTypes.addValue(DfValueFactory.newIntValue(p.getPermitType()));
			permitValues.addValue(DfValueFactory.newStringValue(p.getPermitValueString()));
		}
		properties.add(accessors);
		properties.add(permitValues);
		properties.add(permitTypes);
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
				if (CmsMappingUtils.isMappableUser(session, name)) {
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
			if (obj == null) {
				this.log.warn(String.format("Missing dependency for ACL [%s] - %s [%s] not found (as ACL accessor)",
					acl.getObjectName(), (group ? "group" : "user"), name));
				continue;
			}
			dependencyManager.persistRelatedObject(obj);
		}

		// Do the owner
		final String owner = acl.getDomain();
		if (CmsMappingUtils.isMappableUser(session, owner)) {
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
			user = CmsMappingUtils.resolveMappableUser(acl.getSession(), user);
			acl.setDomain(user);
			acl.setObjectName(getAttribute(CmsAttributes.OBJECT_NAME).getValue().asString());
			acl.save();
		}
		CmsProperty usersWithDefaultACL = getProperty(CmsACL.USERS_WITH_DEFAULT_ACL);
		if (usersWithDefaultACL != null) {
			final IDfSession session = acl.getSession();
			for (IDfValue value : CmsMappingUtils.resolveMappableUsers(acl, usersWithDefaultACL)) {

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
		CmsProperty permitTypes = getProperty(CmsACL.PERMIT_TYPE);
		if (this.log.isTraceEnabled()) {
			this.log.trace(String.format("[%s]: %s", getLabel(), permitTypes));
		}
		CmsProperty permitValues = getProperty(CmsACL.PERMIT_VALUE);
		if (this.log.isTraceEnabled()) {
			this.log.trace(String.format("[%s]: %s", getLabel(), permitValues));
		}
		final int accessorCount = accessors.getValueCount();
		IDfSession session = acl.getSession();
		List<IDfPermit> extendedPerms = new ArrayList<IDfPermit>(accessorCount);
		DfPermit p = new DfPermit();
		for (int i = 0; i < accessorCount; i++) {
			String name = accessors.getValue(i).asString();
			int type = permitTypes.getValue(i).asInteger();
			String perm = permitValues.getValue(i).asString();

			// In addition, need to check for groups for these types
			boolean group = false;
			switch (p.getPermitType()) {
				case IDfPermit.DF_REQUIRED_GROUP:
				case IDfPermit.DF_REQUIRED_GROUP_SET:
					group = true;
					break;
			}

			// Before we check if it's a special name, we must resolve
			// it to its true value if necessary. We only do this for
			// users.
			if (!group) {
				name = CmsMappingUtils.resolveMappableUser(session, name);
			}

			boolean exists = false;
			String accessorType = null;
			if (!CmsMappingUtils.SPECIAL_NAMES.contains(name)) {
				if (group) {
					accessorType = "group";
					exists = (acl.getSession().getGroup(name) != null);
				} else {
					accessorType = "user";
					exists = (acl.getSession().getUser(name) != null);
					// Safety net?
					if (!exists) {
						// This shouldn't be necessary
						this.log
						.warn(String
							.format(
								"ACL [%s] references the user %s, but it wasn't found - will try to search for a group instead",
								getLabel(), name));
						exists = (acl.getSession().getGroup(name) != null);
						accessorType = "accessor (user or group)";
					}
				}
			} else {
				exists = true;
				accessorType = "[SPECIAL]";
			}

			if (!exists) {
				this.log.warn(String.format(
					"ACL [%s] references the %s [%s] for %s permission %s, but the %1$s wasn't found", getLabel(),
					accessorType, name, type, perm));
				continue;
			}

			if (this.log.isDebugEnabled()) {
				this.log.debug(String.format("PERMIT GRANTED on [%s]: [%s|%s|%s]", getLabel(), name, type, perm));
			}

			p.setAccessorName(name);
			p.setPermitType(type);
			p.setPermitValue(perm);

			if ((type == IDfPermit.DF_EXTENDED_PERMIT) || (type == IDfPermit.DF_EXTENDED_RESTRICTION)) {
				extendedPerms.add(p);
				// We need a new object...
				p = new DfPermit();
				continue;
			}

			acl.grantPermit(p);

			// Sadly, these need to be revoked - see below
			p.setPermitType(IDfPermit.DF_EXTENDED_PERMIT);
			p.setPermitValue(IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR);
			acl.revokePermit(p);
			p.setPermitValue(IDfACL.DF_XPERMIT_EXECUTE_PROC_STR);
			acl.revokePermit(p);
		}
		// We have to do it in two passes because EMC (god bless their souls) decided that it
		// would be wise for additional extended permissions to be added automatically as part
		// of adding regular permissions. So in the above cycle we defer the granting of those
		// extended permissions so we can clean out the extended permissions that are added
		// automatically (should they be granted), and proceed to grant the *CORRECT* extended
		// permissions on a separate loop. Thank you, EMC.
		for (IDfPermit permit : extendedPerms) {
			acl.grantPermit(permit);
		}
	}
}