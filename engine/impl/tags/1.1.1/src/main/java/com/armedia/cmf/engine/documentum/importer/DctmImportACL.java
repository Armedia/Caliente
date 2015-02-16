/**
 *
 */

package com.armedia.cmf.engine.documentum.importer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.armedia.cmf.engine.documentum.DctmAttributes;
import com.armedia.cmf.engine.documentum.DctmMappingUtils;
import com.armedia.cmf.engine.documentum.DctmObjectType;
import com.armedia.cmf.engine.documentum.DfUtils;
import com.armedia.cmf.engine.documentum.common.DctmACL;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.storage.StoredAttribute;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredProperty;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.DfACLException;
import com.documentum.fc.client.DfPermit;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfPermit;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfList;
import com.documentum.fc.common.IDfValue;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class DctmImportACL extends DctmImportDelegate<IDfACL> implements DctmACL {

	private static class Permit implements Comparable<Permit> {
		private final String name;
		private final int type;
		private final String typeStr;
		private final String value;

		private Permit(IDfSession session, IDfPermit permit) throws DfException {
			this.name = permit.getAccessorName();
			this.type = permit.getPermitType();
			this.value = permit.getPermitValueString();
			this.typeStr = DfUtils.decodePermitType(this.type);
		}

		private Permit(IDfSession session, DctmImportACL aclDelegate, int pos) throws DfException {
			StoredObject<IDfValue> acl = aclDelegate.storedObject;
			StoredProperty<IDfValue> prop = null;
			prop = acl.getProperty(DctmACL.ACCESSORS);
			this.name = DctmMappingUtils.resolveMappableUser(session, prop.getValue(pos).asString());
			prop = acl.getProperty(DctmACL.PERMIT_TYPE);
			this.type = prop.getValue(pos).asInteger();
			prop = acl.getProperty(DctmACL.PERMIT_VALUE);
			this.value = prop.getValue(pos).asString();
			this.typeStr = DfUtils.decodePermitType(this.type);
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
			return String.format("Permit [name=%s, type=%s, value=%s]", this.name, this.typeStr, this.value);
		}
	}

	protected DctmImportACL(DctmImportEngine engine, StoredObject<IDfValue> storedObject) {
		super(engine, DctmObjectType.ACL, storedObject);
	}

	@Override
	protected String calculateLabel(IDfACL acl) throws DfException {
		return String.format("%s::%s", acl.getDomain(), acl.getObjectName());
	}

	@Override
	protected IDfACL locateInCms(DctmImportContext ctx) throws DfException {
		final IDfValue ownerName = this.storedObject.getAttribute(DctmAttributes.OWNER_NAME).getValue();
		final IDfValue objectName = this.storedObject.getAttribute(DctmAttributes.OBJECT_NAME).getValue();
		final IDfSession session = ctx.getSession();
		return session.getACL(ownerName != null ? DctmMappingUtils.resolveMappableUser(session, ownerName.asString())
			: null, objectName.asString());
	}

	@Override
	protected boolean isSameObject(IDfACL acl) throws DfException {
		// ACL's don't have a modification date, so we actually have to compare them.
		// If this is a potential match, then it was found using domain and object name,
		// so we don't need to check those two, but we do need to check the permissions
		// granted to see if they match. In particular, the target ACL may be a superset
		// of the one we're trying to bring in on top of it, but never a subset.
		StoredAttribute<IDfValue> att = null;

		att = this.storedObject.getAttribute(DctmAttributes.ACL_CLASS);
		if (att.getValue().asInteger() != acl.getACLClass()) { return false; }

		att = this.storedObject.getAttribute(DctmAttributes.GLOBALLY_MANAGED);
		if (att.getValue().asBoolean() != acl.isGloballyManaged()) { return false; }

		att = this.storedObject.getAttribute(DctmAttributes.I_HAS_ACCESS_RESTRICTIONS);
		if (att.getValue().asBoolean() != acl.getBoolean(att.getName())) { return false; }

		att = this.storedObject.getAttribute(DctmAttributes.I_HAS_REQUIRED_GROUPS);
		if (att.getValue().asBoolean() != acl.getBoolean(att.getName())) { return false; }

		att = this.storedObject.getAttribute(DctmAttributes.I_HAS_REQUIRED_GROUP_SET);
		if (att.getValue().asBoolean() != acl.getBoolean(att.getName())) { return false; }

		// Now, count the permits
		IDfList existingPermits = acl.getPermissions();
		StoredProperty<IDfValue> prop = this.storedObject.getProperty(DctmACL.ACCESSORS);
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
	protected void finalizeConstruction(IDfACL acl, boolean newObject, DctmImportContext context) throws DfException,
		ImportException {
		if (newObject) {
			String user = this.storedObject.getAttribute(DctmAttributes.OWNER_NAME).getValue().asString();
			user = DctmMappingUtils.resolveMappableUser(acl.getSession(), user);
			acl.setDomain(user);
			acl.setObjectName(this.storedObject.getAttribute(DctmAttributes.OBJECT_NAME).getValue().asString());
			acl.save();
		}

		// Clear any existing permissions
		final IDfList existingPermissions = acl.getPermissions();
		final int existingPermissionCount = existingPermissions.getCount();
		final IDfSession session = acl.getSession();
		for (int i = 0; i < existingPermissionCount; i++) {
			IDfPermit permit = IDfPermit.class.cast(existingPermissions.get(i));
			try {
				acl.revokePermit(permit);
				if (this.log.isDebugEnabled()) {
					this.log.debug(String.format("PERMIT REVOKED on [%s]: [%s|%d|%d (%s)]",
						this.storedObject.getLabel(), permit.getAccessorName(), permit.getPermitType(),
						permit.getPermitValueInt(), permit.getPermitValueString()));
				}
			} catch (DfACLException e) {
				if ("DM_ACL_E_NOMATCH".equals(e.getMessageId())) {
					// we can survive this...
					this.log
						.warn(String
							.format(
								"PERMIT REVOKATION FAILED on [%s]: [%s|%d|%d (%s)] - ACE not found, possibly removed implicitly",
								this.storedObject.getLabel(), permit.getAccessorName(), permit.getPermitType(),
								permit.getPermitValueInt(), permit.getPermitValueString()));
					continue;
				}
				// something else? don't snuff it...
				throw e;
			}
		}

		// Now, apply the new permissions
		StoredProperty<IDfValue> accessors = this.storedObject.getProperty(DctmACL.ACCESSORS);
		if (this.log.isTraceEnabled()) {
			this.log.trace(String.format("[%s]: %s", this.storedObject.getLabel(), accessors));
		}
		StoredProperty<IDfValue> permitTypes = this.storedObject.getProperty(DctmACL.PERMIT_TYPE);
		if (this.log.isTraceEnabled()) {
			this.log.trace(String.format("[%s]: %s", this.storedObject.getLabel(), permitTypes));
		}
		StoredProperty<IDfValue> permitValues = this.storedObject.getProperty(DctmACL.PERMIT_VALUE);
		if (this.log.isTraceEnabled()) {
			this.log.trace(String.format("[%s]: %s", this.storedObject.getLabel(), permitValues));
		}

		// If all 3 are null, then we assume an empty list
		if ((accessors == null) && (permitTypes == null) && (permitValues == null)) {
			if (this.log.isDebugEnabled()) {
				this.log.warn("Empty ACL created at [{}]({})", this.storedObject.getLabel(), this.storedObject.getId());
			}
			return;
		}

		// Ok...so at least "some" of the properties are there, so we validate that they
		// all have the proper structure: none of them are missing, and they all have the
		// same number of values
		if ((accessors == null) || (permitTypes == null) || (permitValues == null)
			|| (accessors.getValueCount() != permitTypes.getValueCount())
			|| (accessors.getValueCount() != permitValues.getValueCount())) { throw new ImportException(String.format(
			"Irregular ACL data stored for ACL [%s](%s)%naccessors = %s%permitType = %s%npermitValue = %s",
			this.storedObject.getLabel(), this.storedObject.getId(), accessors, permitTypes, permitValues)); }

		// One final check to shortcut and avoid unnecessary processing...
		final int accessorCount = accessors.getValueCount();
		if (accessorCount == 0) {
			if (this.log.isDebugEnabled()) {
				this.log.warn("Empty ACL created at [{}]({})", this.storedObject.getLabel(), this.storedObject.getId());
			}
			return;
		}

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
				name = DctmMappingUtils.resolveMappableUser(session, name);
			}

			boolean exists = false;
			String accessorType = null;
			if (!DctmMappingUtils.SPECIAL_NAMES.contains(name)) {
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
									this.storedObject.getLabel(), name));
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
					"ACL [%s] references the %s [%s] for %s permission %s, but the %1$s wasn't found",
					this.storedObject.getLabel(), accessorType, name, type, perm));
				continue;
			}

			if (this.log.isDebugEnabled()) {
				this.log.debug(String.format("PERMIT GRANTED on [%s]: [%s|%s|%s]", this.storedObject.getLabel(), name,
					type, perm));
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

	@Override
	protected void updateReferenced(IDfACL acl, DctmImportContext context) throws DfException, ImportException {
		final StoredProperty<IDfValue> usersWithDefaultACL = this.storedObject
			.getProperty(DctmACL.USERS_WITH_DEFAULT_ACL);
		if ((usersWithDefaultACL == null) || (usersWithDefaultACL.getValueCount() == 0)) { return; }

		final IDfSession session = context.getSession();
		for (IDfValue value : DctmMappingUtils.resolveMappableUsers(acl, usersWithDefaultACL)) {

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
			user.lock();
			user.fetch(null);
			user.setDefaultACLEx(acl.getDomain(), acl.getObjectName());
			user.save();
			// Update the system attributes, if we can
			try {
				updateSystemAttributes(user, context);
			} catch (ImportException e) {
				this.log
					.warn(
						String
							.format(
								"Failed to update the system attributes for user [%s] after assigning ACL [%s] as their default ACL",
								user.getUserName(), this.storedObject.getLabel()), e);
			}
		}
	}
}