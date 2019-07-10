/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
/**
 *
 */

package com.armedia.caliente.engine.dfc.importer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.dfc.DctmAttributes;
import com.armedia.caliente.engine.dfc.DctmMappingUtils;
import com.armedia.caliente.engine.dfc.DctmObjectType;
import com.armedia.caliente.engine.dfc.common.DctmACL;
import com.armedia.caliente.engine.dfc.common.DctmCmisACLTools;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.tools.dfc.DfcUtils;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.DfACLException;
import com.documentum.fc.client.DfPermit;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfGroup;
import com.documentum.fc.client.IDfPermit;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfValue;

/**
 *
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
			this.typeStr = DfcUtils.decodePermitType(this.type);
		}

		private Permit(IDfSession session, DctmImportACL aclDelegate, int pos) throws DfException {
			CmfObject<IDfValue> acl = aclDelegate.cmfObject;
			CmfProperty<IDfValue> prop = null;
			prop = acl.getProperty(DctmACL.ACCESSORS);
			this.name = DctmMappingUtils.resolveMappableUser(session, prop.getValue(pos).asString());
			prop = acl.getProperty(DctmACL.PERMIT_TYPES);
			this.type = prop.getValue(pos).asInteger();
			prop = acl.getProperty(DctmACL.PERMIT_VALUES);
			this.value = prop.getValue(pos).asString();
			this.typeStr = DfcUtils.decodePermitType(this.type);
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

	private final boolean documentumMode;

	protected DctmImportACL(DctmImportDelegateFactory factory, CmfObject<IDfValue> storedObject) throws Exception {
		super(factory, IDfACL.class, DctmObjectType.ACL, storedObject);
		CmfProperty<IDfValue> prop = storedObject.getProperty(DctmACL.DOCUMENTUM_MARKER);
		this.documentumMode = ((prop != null) && prop.hasValues() && prop.getValue().asBoolean());
	}

	@Override
	protected String calculateLabel(IDfACL acl) throws DfException {
		return String.format("%s::%s", acl.getDomain(), acl.getObjectName());
	}

	@Override
	protected IDfACL locateInCms(DctmImportContext ctx) throws DfException {
		if (!this.documentumMode) {
			// TODO: If the incoming ACL isn't a documentum-exported ACL, how the hell do we
			// find the "existing" ACL to compare it with?
			return null;
		}
		final IDfValue ownerName = this.cmfObject.getAttribute(DctmAttributes.OWNER_NAME).getValue();
		final IDfValue objectName = this.cmfObject.getAttribute(DctmAttributes.OBJECT_NAME).getValue();
		final IDfSession session = ctx.getSession();
		return session.getACL(
			ownerName != null ? DctmMappingUtils.resolveMappableUser(session, ownerName.asString()) : null,
			objectName.asString());
	}

	@Override
	protected boolean isSameObject(IDfACL acl, DctmImportContext ctx) throws DfException {
		// ACL's don't have a modification date, so we actually have to compare them.
		// If this is a potential match, then it was found using domain and object name,
		// so we don't need to check those two, but we do need to check the permissions
		// granted to see if they match. In particular, the target ACL may be a superset
		// of the one we're trying to bring in on top of it, but never a subset.
		CmfAttribute<IDfValue> att = null;

		att = this.cmfObject.getAttribute(DctmAttributes.ACL_CLASS);
		if (att.getValue().asInteger() != acl.getACLClass()) { return false; }

		att = this.cmfObject.getAttribute(DctmAttributes.GLOBALLY_MANAGED);
		if (att.getValue().asBoolean() != acl.isGloballyManaged()) { return false; }

		att = this.cmfObject.getAttribute(DctmAttributes.I_HAS_ACCESS_RESTRICTIONS);
		if (att.getValue().asBoolean() != acl.getBoolean(att.getName())) { return false; }

		att = this.cmfObject.getAttribute(DctmAttributes.I_HAS_REQUIRED_GROUPS);
		if (att.getValue().asBoolean() != acl.getBoolean(att.getName())) { return false; }

		att = this.cmfObject.getAttribute(DctmAttributes.I_HAS_REQUIRED_GROUP_SET);
		if (att.getValue().asBoolean() != acl.getBoolean(att.getName())) { return false; }

		// Now, count the permits
		Collection<IDfPermit> existingPermits = DfcUtils.getPermissionsWithFallback(ctx.getSession(), acl);
		CmfProperty<IDfValue> prop = this.cmfObject.getProperty(DctmACL.ACCESSORS);
		if (existingPermits.size() != prop.getValueCount()) {
			// The permit counts have to be identical...
			return false;
		}

		Set<Permit> existing = new HashSet<>();
		Set<Permit> incoming = new HashSet<>();
		IDfSession session = ctx.getSession();
		int i = 0;
		for (IDfPermit p : existingPermits) {
			existing.add(new Permit(session, p));
			incoming.add(new Permit(session, this, i++));
		}

		// The ACLs are considered the same only if the sets are identical
		return incoming.equals(existing);
	}

	@Override
	protected void finalizeConstruction(IDfACL acl, boolean newObject, DctmImportContext context)
		throws DfException, ImportException {
		if (newObject) {
			CmfAttribute<IDfValue> att = this.cmfObject.getAttribute(DctmAttributes.OWNER_NAME);
			String user = null;
			if ((att != null) && att.hasValues()) {
				user = att.getValue().asString();
			} else {
				CmfProperty<IDfValue> prop = this.cmfObject.getProperty(IntermediateProperty.ACL_OWNER);
				if ((prop != null) && prop.hasValues()) {
					user = prop.getValue().asString();
				}
			}
			att = this.cmfObject.getAttribute(DctmAttributes.OBJECT_NAME);
			String name = null;
			if ((att != null) && att.hasValues()) {
				name = att.getValue().asString();
			} else {
				CmfProperty<IDfValue> prop = this.cmfObject.getProperty(IntermediateProperty.ACL_OBJECT_ID);
				if ((prop != null) && prop.hasValues()) {
					name = prop.getValue().asString();
				}
			}

			user = DctmMappingUtils.resolveMappableUser(context.getSession(), user);
			IDfUser u = DctmImportUser.locateExistingUser(context, user);
			if (u == null) {
				throw new ImportException(
					String.format("Failed to locate the owner [%s] for %s", user, this.cmfObject.getDescription()));
			}
			acl.setDomain(u.getUserName());
			acl.setObjectName(name);
			acl.save();
		}

		// Clear any existing permissions
		final IDfSession session = acl.getSession();
		for (IDfPermit permit : DfcUtils.getPermissionsWithFallback(context.getSession(), acl)) {
			try {
				acl.revokePermit(permit);
				if (this.log.isDebugEnabled()) {
					this.log.debug("PERMIT REVOKED on [{}]: [{}|{}|{} ({})]", this.cmfObject.getLabel(),
						permit.getAccessorName(), permit.getPermitType(), permit.getPermitValueInt(),
						permit.getPermitValueString());
				}
			} catch (DfACLException e) {
				if (StringUtils.equalsIgnoreCase("DM_ACL_E_NOMATCH", e.getMessageId())) {
					// we can survive this...
					this.log.warn(
						"PERMIT REVOKATION FAILED on [{}]: [{}|{}|{} ({})] - ACE not found, possibly removed implicitly",
						this.cmfObject.getLabel(), permit.getAccessorName(), permit.getPermitType(),
						permit.getPermitValueInt(), permit.getPermitValueString());
					continue;
				}
				// something else? don't snuff it...
				throw e;
			}
		}

		// Now, apply the new permissions
		List<IDfPermit> extendedPerms = new ArrayList<>();
		if (this.documentumMode) {
			CmfProperty<IDfValue> accessors = this.cmfObject.getProperty(DctmACL.ACCESSORS);
			if (this.log.isTraceEnabled()) {
				this.log.trace("[{}]: {}", this.cmfObject.getLabel(), accessors);
			}
			CmfProperty<IDfValue> permitTypes = this.cmfObject.getProperty(DctmACL.PERMIT_TYPES);
			if (this.log.isTraceEnabled()) {
				this.log.trace("[{}]: {}", this.cmfObject.getLabel(), permitTypes);
			}
			CmfProperty<IDfValue> permitValues = this.cmfObject.getProperty(DctmACL.PERMIT_VALUES);
			if (this.log.isTraceEnabled()) {
				this.log.trace("[{}]: {}", this.cmfObject.getLabel(), permitValues);
			}

			// If all 3 are null, then we assume an empty list
			if ((accessors == null) && (permitTypes == null) && (permitValues == null)) {
				if (this.log.isDebugEnabled()) {
					this.log.warn("Created empty {}", this.cmfObject.getDescription());
				}
				return;
			}

			// Ok...so at least "some" of the properties are there, so we validate that they
			// all have the proper structure: none of them are missing, and they all have the
			// same number of values
			if ((accessors == null) || (permitTypes == null) || (permitValues == null)
				|| (accessors.getValueCount() != permitTypes.getValueCount())
				|| (accessors.getValueCount() != permitValues.getValueCount())) {
				throw new ImportException(
					String.format("Irregular ACL data stored for %s%naccessors = %s%npermitType = %s%npermitValue = %s",
						this.cmfObject.getDescription(), accessors, permitTypes, permitValues));
			}

			// One final check to shortcut and avoid unnecessary processing...
			final int accessorCount = accessors.getValueCount();
			if (accessorCount == 0) {
				if (this.log.isDebugEnabled()) {
					this.log.warn("Created empty []", this.cmfObject.getDescription());
				}
				return;
			}

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
						exists = (session.getGroup(name) != null);
					} else {
						accessorType = "user";
						try {
							IDfUser u = DctmImportUser.locateExistingUser(context, name);
							if (u != null) {
								name = u.getUserName();
								exists = true;
							} else {
								exists = false;
							}
						} catch (ImportException e) {
							this.log.warn("ACL [{}] references the user {} - {} - will not add the accessor to the ACL",
								this.cmfObject.getLabel(), name, e.getMessage());
							continue;
						}
						// Safety net?
						if (!exists) {
							// This shouldn't be necessary
							this.log.warn(
								"ACL [{}] references the user {}, but it wasn't found - will try to search for a group instead",
								this.cmfObject.getLabel(), name);
							exists = (session.getGroup(name) != null);
							accessorType = "accessor (user or group)";
						}
					}
				} else {
					exists = true;
					accessorType = "[SPECIAL]";
				}

				if (!exists) {
					this.log.warn("ACL [{}] references the {} [{}] for {} permission {}, but the {} wasn't found",
						this.cmfObject.getLabel(), accessorType, name, type, perm, accessorType);
					continue;
				}

				if (this.log.isDebugEnabled()) {
					this.log.debug("PERMIT GRANTED on [{}]: [{}|{}|{}]", this.cmfObject.getLabel(), name, type, perm);
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
		} else {
			// In CMIS mode, we simply get the list of permits to grant, ordered by accessor
			for (IDfPermit p : DctmCmisACLTools.calculatePermissionsFromCMIS(this.cmfObject)) {

				// First, validate if this principal is a valid user/group
				IDfUser u = session.getUser(p.getAccessorName());
				IDfGroup g = session.getGroup(p.getAccessorName());
				if ((u == null) && (g == null)) {
					// Missing principal...forget it
					continue;
				}

				switch (p.getPermitType()) {
					case IDfPermit.DF_ACCESS_PERMIT:
						acl.grantPermit(p);

						// Sadly, these need to be revoked - see below
						p.setPermitType(IDfPermit.DF_EXTENDED_PERMIT);
						p.setPermitValue(IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR);
						acl.revokePermit(p);
						p.setPermitValue(IDfACL.DF_XPERMIT_EXECUTE_PROC_STR);
						acl.revokePermit(p);
						break;

					case IDfPermit.DF_EXTENDED_PERMIT:
						extendedPerms.add(p);
						break;

					default:
						// Ignore it...don't know what the hell it is...
						continue;
				}
			}

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
		final CmfProperty<IDfValue> usersWithDefaultACL = this.cmfObject
			.getProperty(IntermediateProperty.USERS_WITH_DEFAULT_ACL);
		if ((usersWithDefaultACL == null) || (usersWithDefaultACL.getValueCount() == 0)) { return; }

		final IDfSession session = context.getSession();
		Set<String> users = new TreeSet<>();
		for (IDfValue value : usersWithDefaultACL) {
			String userName = value.asString();
			// Don't touch the special users!!
			if (context.isUntouchableUser(userName)) {
				this.log.warn("Will not substitute the default ACL for the special user [{}]",
					DctmMappingUtils.resolveMappableUser(session, userName));
				continue;
			}
			users.add(value.asString());
		}

		// First we sort the user names to ensure we don't have deadlocks - since we lock the users
		// in the next loop, it's CRITICAL for us to ensure we lock them in the same order
		for (String userName : users) {

			// TODO: How do we decide if we should update the default ACL for this user? What if
			// the user's default ACL has been modified on the target CMS and we don't want to
			// clobber that?
			final IDfUser user;
			try {
				user = DctmImportUser.locateExistingUser(context, userName);
			} catch (MultipleUserMatchesException e) {
				this.log.warn("Failed to link ACL [{}.{}] to user [{}] as its default ACL - {}", acl.getDomain(),
					acl.getObjectName(), userName, e.getMessage());
				continue;
			}

			if (user == null) {
				this.log.warn("Failed to link ACL [{}.{}] to user [{}] as its default ACL - the user wasn't found",
					acl.getDomain(), acl.getObjectName(), userName);
				continue;
			}

			// Ok...so we relate this thing back to its owner as its internal ACL
			DfcUtils.lockObject(this.log, user);
			user.fetch(null);
			user.setDefaultACLEx(acl.getDomain(), acl.getObjectName());
			user.save();
			// Update the system attributes, if we can
			try {
				updateSystemAttributes(user, context);
			} catch (ImportException e) {
				this.log.warn(
					"Failed to update the system attributes for user [{}] after assigning ACL [{}] as their default ACL",
					user.getUserName(), this.cmfObject.getLabel(), e);
			}
		}
	}
}