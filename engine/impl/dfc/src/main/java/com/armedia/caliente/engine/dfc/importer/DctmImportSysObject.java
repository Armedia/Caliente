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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.dfc.DctmAttributes;
import com.armedia.caliente.engine.dfc.DctmMappingUtils;
import com.armedia.caliente.engine.dfc.DctmObjectType;
import com.armedia.caliente.engine.dfc.DctmTranslator;
import com.armedia.caliente.engine.dfc.common.DctmSysObject;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.engine.importer.ImportOutcome;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueMapper.Mapping;
import com.armedia.caliente.tools.dfc.DfValueFactory;
import com.armedia.caliente.tools.dfc.DfcQuery;
import com.armedia.caliente.tools.dfc.DfcUtils;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.DfObjectNotFoundException;
import com.documentum.fc.client.DfPermit;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfGroup;
import com.documentum.fc.client.IDfPermit;
import com.documentum.fc.client.IDfPermitType;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.IDfType;
import com.documentum.fc.client.IDfTypedObject;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.client.distributed.IDfReference;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfValue;

public abstract class DctmImportSysObject<T extends IDfSysObject> extends DctmImportDelegate<T>
	implements DctmSysObject {

	// Disable, for now, since it messes up with version number copying
	// private static final Pattern INTERNAL_VL = Pattern.compile("^\\d+(\\.\\d+)+$");
	private static final Collection<String> NO_PERMITS = Collections.emptySet();

	protected static final String BRANCH_MARKER = "branchMarker";

	private static final Pattern ACL_INHERITANCE_PARSER = Pattern.compile("^(\\w+)\\[(.*)\\]$");

	private static final Set<String> AUTO_PERMITS;

	static {
		Set<String> s = new HashSet<>();
		s.add(IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR);
		s.add(IDfACL.DF_XPERMIT_EXECUTE_PROC_STR);
		AUTO_PERMITS = Collections.unmodifiableSet(s);
	}

	private static Set<String> getSupportedExtendedPermits(IDfSession session, IDfSysObject sysObject)
		throws DfException {
		final Set<String> ret = new HashSet<>();

		DfException e = DfcUtils.runRetryable(session,
			(s) -> ret.addAll(StringTokenizer.getCSVInstance(sysObject.getXPermitList()).getTokenList()));
		if ((e != null) && !StringUtils.equalsIgnoreCase("DM_SYSOBJECT_W_FOLDER_DEFACL", e.getMessageId())) { throw e; }

		return ret;
	}

	protected static final class TemporaryPermission {
		private final Logger log = LoggerFactory.getLogger(getClass());

		private final String objectId;
		private final IDfPermit oldPermit;
		private final IDfPermit newPermit;
		private final Set<String> newXPermit;
		private final Set<String> autoRemove;

		public TemporaryPermission(IDfSession session, IDfSysObject object, int newPermission, String... newXPermits)
			throws DfException {
			this(session, object, newPermission,
				(newXPermits == null ? DctmImportSysObject.NO_PERMITS : Arrays.asList(newXPermits)));
		}

		public TemporaryPermission(IDfSession session, IDfSysObject object, int newPermission,
			Collection<String> newXPermits) throws DfException {
			this.objectId = object.getObjectId().getId();
			final String userName = session.getLoginUserName();

			// Does it have the required access permission?
			object.fetch(null);
			int oldPermission = object.getPermitEx(userName);
			if (oldPermission < newPermission) {
				if (oldPermission > 0) {
					this.oldPermit = new DfPermit();
					this.oldPermit.setAccessorName(userName);
					this.oldPermit.setPermitType(IDfPermitType.ACCESS_PERMIT);
					this.oldPermit.setPermitValue(DfcUtils.decodeAccessPermission(oldPermission));
				} else {
					this.oldPermit = null;
				}
				this.newPermit = new DfPermit();
				this.newPermit.setAccessorName(userName);
				this.newPermit.setPermitType(IDfPermitType.ACCESS_PERMIT);
				this.newPermit.setPermitValue(DfcUtils.decodeAccessPermission(newPermission));
			} else {
				this.oldPermit = null;
				this.newPermit = null;
			}

			Set<String> s = DctmImportSysObject.getSupportedExtendedPermits(session, object);
			Set<String> autoRemove = new HashSet<>();
			for (String x : DctmImportSysObject.AUTO_PERMITS) {
				if (!s.contains(x)) {
					autoRemove.add(x);
				}
			}
			this.autoRemove = Collections.unmodifiableSet(autoRemove);

			// Now the ones we're adding
			Set<String> nx = new TreeSet<>();
			for (String x : newXPermits) {
				if (x == null) {
					continue;
				}
				if (s.contains(x)) {
					continue;
				}
				nx.add(x);
			}
			this.newXPermit = Collections.unmodifiableSet(nx);
		}

		public String getObjectId() {
			return this.objectId;
		}

		private boolean apply(IDfSysObject object, boolean grant) throws DfException {
			if (!Objects.equals(this.objectId, object.getObjectId().getId())) {
				throw new DfException(String.format("ERROR: Expected object with ID [%s] but got [%s] instead",
					this.objectId, object.getObjectId().getId()));
			}
			boolean ret = false;
			if (this.newPermit != null) {
				IDfPermit toGrant = (grant ? this.newPermit : this.oldPermit);
				IDfPermit toRevoke = (grant ? this.oldPermit : this.newPermit);
				if (toRevoke != null) {
					if (this.log.isDebugEnabled()) {
						this.log.debug("REVOKING [{}] on [{}]", toRevoke.getPermitValueString(), object.getObjectId());
					}
					object.revokePermit(toRevoke);
				}
				if (toGrant != null) {
					if (this.log.isDebugEnabled()) {
						this.log.debug("GRANTING [{}] on [{}]", toGrant.getPermitValueString(), object.getObjectId());
					}
					object.grantPermit(toGrant);
				}

				// Sadly, if we're messing with access permissions, we may have to clear these out
				// if they're added automagically by Documentum
				IDfPermit auto = null;
				for (String p : this.autoRemove) {
					if (auto == null) {
						auto = new DfPermit();
						auto.setAccessorName(this.newPermit.getAccessorName());
						auto.setPermitType(IDfPermit.DF_EXTENDED_PERMIT);
					}
					auto.setPermitValue(p);
					if (this.log.isDebugEnabled()) {
						this.log.debug("REVOKING AUTO-XPERM [{}] on [{}]", auto.getPermitValueString(),
							object.getObjectId());
					}
					object.revokePermit(auto);
				}
				ret = true;
			}

			// Apply the x-permit deltas
			IDfPermit xperm = null;
			for (String p : this.newXPermit) {
				if (xperm == null) {
					xperm = new DfPermit();
					xperm.setAccessorName(this.newPermit.getAccessorName());
					xperm.setPermitType(IDfPermit.DF_EXTENDED_PERMIT);
				}
				xperm.setPermitValue(p);

				if (this.log.isDebugEnabled()) {
					this.log.debug("{} [{}] on [{}]", (grant ? "GRANTING" : "REVOKING"), p, object.getObjectId());
				}
				if (grant) {
					object.grantPermit(xperm);
				} else {
					object.revokePermit(xperm);
				}
				ret = true;
			}
			return ret;
		}

		public boolean grant(IDfSysObject object) throws DfException {
			return apply(object, true);
		}

		public boolean revoke(IDfSysObject object) throws DfException {
			return apply(object, false);
		}

		@Override
		public String toString() {
			String op = (this.oldPermit != null ? this.oldPermit.getPermitValueString() : "(N/A)");
			String np = (this.newPermit != null ? this.newPermit.getPermitValueString() : "(N/A)");
			return String.format(
				"TemporaryPermission [objectId=%s, oldPermit=%s, newPermit=%s, newXPermit=%s, autoRemove=%s]",
				this.objectId, op, np, this.newXPermit, this.autoRemove);
		}
	}

	protected static final class ParentFolderAction implements Comparable<ParentFolderAction> {
		private final Logger log = LoggerFactory.getLogger(getClass());

		private final IDfSysObject parent;
		private final String parentId;
		private final TemporaryPermission TemporaryPermission;
		private final boolean link;
		private boolean locked = false;

		protected ParentFolderAction(IDfSession session, IDfSysObject parent, boolean link) throws DfException {
			this.parent = parent;
			this.parentId = parent.getObjectId().getId();
			this.link = link;
			this.TemporaryPermission = new TemporaryPermission(session, parent, IDfACL.DF_PERMIT_DELETE,
				IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR);
		}

		private void ensureLocked() throws DfException {
			if (!this.locked) {
				DfcUtils.lockObject(this.log, this.parent);
				this.locked = true;
			}
		}

		protected void apply(IDfSysObject child) throws DfException {
			ensureLocked();
			if (this.TemporaryPermission.grant(this.parent)) {
				this.parent.save();
			}
			if (this.log.isDebugEnabled()) {
				this.log.debug("{}LINKING [{}] --> [{}]", this.link ? "" : "UN", child.getObjectId().getId(),
					this.parent.getObjectId().getId());
			}
			if (this.link) {
				child.link(this.parentId);
			} else {
				child.unlink(this.parentId);
			}
		}

		protected void cleanUp() throws DfException {
			ensureLocked();
			if (this.TemporaryPermission.revoke(this.parent)) {
				this.parent.save();
			}
		}

		@Override
		public int compareTo(ParentFolderAction o) {
			// Sort unlinks before links
			if (this.link != o.link) { return (this.link ? 1 : -1); }
			return Tools.compare(this.parentId, o.parentId);
		}

		@Override
		public int hashCode() {
			return Tools.hashTool(this, null, this.parentId, this.link);
		}

		@Override
		public boolean equals(Object obj) {
			if (!Tools.baseEquals(this, obj)) { return false; }
			ParentFolderAction other = ParentFolderAction.class.cast(obj);
			if (this.link != other.link) { return false; }
			if (!Objects.equals(this.parentId, other.parentId)) { return false; }
			return true;
		}

		@Override
		public String toString() {
			return String.format("ParentFolderAction [parentId=%s, link=%s, locked=%s, TemporaryPermission=%s]",
				this.parentId, this.link, this.locked, this.TemporaryPermission);
		}
	}

	private boolean mutabilityModified = false;
	private boolean mustFreeze = false;
	private boolean mustImmute = false;
	private TemporaryPermission existingTemporaryPermission = null;
	private Collection<ParentFolderAction> parentLinkActions = null;

	public DctmImportSysObject(DctmImportDelegateFactory factory, Class<T> objectClass, DctmObjectType type,
		CmfObject<IDfValue> storedObject) throws Exception {
		super(factory, objectClass, type, storedObject);
	}

	@Override
	protected boolean isTransitoryObject(T sysObject) throws DfException, ImportException {
		return sysObject.isCheckedOut();
	}

	protected final void detectIncomingMutability() {
		if (!this.mustFreeze) {
			CmfAttribute<IDfValue> frozen = this.cmfObject.getAttribute(DctmAttributes.R_FROZEN_FLAG);
			if (frozen != null) {
				// We only copy over the "true" values - we don't override local frozen status
				// if it's set to true, and the incoming value is false
				this.mustFreeze |= frozen.getValue().asBoolean();
			}
		}
		if (!this.mustFreeze) {
			CmfAttribute<IDfValue> immutable = this.cmfObject.getAttribute(DctmAttributes.R_IMMUTABLE_FLAG);
			if (immutable != null) {
				// We only copy over the "true" values - we don't override local immutable
				// status if it's set to true, and the incoming value is false
				this.mustImmute |= immutable.getValue().asBoolean();
			}
		}
	}

	protected final void detectAndClearMutability(T sysObject) throws DfException {
		resetMutabilityFlags();
		final String newId = sysObject.getObjectId().getId();
		if (sysObject.isFrozen()) {
			// An object being frozen implies immutability
			this.mustFreeze = true;
			if (this.log.isDebugEnabled()) {
				this.log.debug("Clearing frozen status from [{}]({}){{}}", this.cmfObject.getLabel(),
					this.cmfObject.getId(), newId);
			}
			// TODO: How to determine if we use false or true here? Stick to false, for now...
			sysObject.unfreeze(false);
			if (!sysObject.isCheckedOut()) {
				sysObject.save();
			}
			this.mutabilityModified = true;
		}
		// Freezing implies immutability
		if (!this.mustFreeze && sysObject.isImmutable()) {
			// An object may be immutable, yet not frozen
			this.mustImmute = true;
			if (this.log.isDebugEnabled()) {
				this.log.debug("Clearing immutable status from [{}]({}){{}}", this.cmfObject.getLabel(),
					this.cmfObject.getId(), newId);
			}
			sysObject.setBoolean(DctmAttributes.R_IMMUTABLE_FLAG, false);
			if (!sysObject.isCheckedOut()) {
				sysObject.save();
			}
			this.mutabilityModified = true;
		}
		detectIncomingMutability();
	}

	protected final boolean restoreMutability(T sysObject) throws DfException {
		if (!this.mutabilityModified) { return false; }
		boolean ret = false;
		final String newId = sysObject.getObjectId().getId();
		if (this.mustFreeze && !sysObject.isFrozen()) {
			if (this.log.isDebugEnabled()) {
				this.log.debug("Setting frozen status to [{}]({}){{}}", this.cmfObject.getLabel(),
					this.cmfObject.getId(), newId);
			}
			// TODO: assembly support?
			// TODO: How to determine if we use false or true here? Stick to false, for now...
			sysObject.freeze(false);
			ret |= true;
		} else if (this.mustImmute && !sysObject.isImmutable()) {
			if (this.log.isDebugEnabled()) {
				this.log.debug("Setting immutability status to [{}]({}){{}}", this.cmfObject.getLabel(),
					this.cmfObject.getId(), newId);
			}
			sysObject.setBoolean(DctmAttributes.R_IMMUTABLE_FLAG, true);
			ret |= true;
		}
		this.mutabilityModified = false;
		return ret;
	}

	protected final void resetMutabilityFlags() {
		this.mustFreeze = false;
		this.mustImmute = false;
		this.mutabilityModified = false;
	}

	@Override
	protected void prepareOperation(T sysObject, boolean newObject, DctmImportContext context)
		throws DfException, ImportException {

		if (!isTransitoryObject(sysObject)) {
			this.existingTemporaryPermission = new TemporaryPermission(context.getSession(), sysObject,
				IDfACL.DF_PERMIT_DELETE);
			if (this.existingTemporaryPermission.grant(sysObject)) {
				sysObject.save();
			}
		}
	}

	protected final boolean copyAcl(IDfPersistentObject source, T target, DctmImportContext ctx)
		throws DfException, ImportException {
		if (IDfSysObject.class.isInstance(source)) { return copyAcl(IDfSysObject.class.cast(source), target, ctx); }
		return copyAcl(source, target, null, null, ctx);
	}

	protected final boolean copyAcl(IDfPersistentObject source, T sysObj, String aclDomainAtt, String aclNameAtt,
		DctmImportContext ctx) throws DfException, ImportException {
		if (source == sysObj) { return true; }
		if ((source == null) || (sysObj == null)) { return true; }
		if (StringUtils.isEmpty(aclDomainAtt)) {
			aclDomainAtt = DctmAttributes.ACL_DOMAIN;
		}
		if (StringUtils.isEmpty(aclNameAtt)) {
			aclNameAtt = DctmAttributes.ACL_NAME;
		}
		if (!source.hasAttr(aclDomainAtt) || !source.hasAttr(aclNameAtt)) {
			ctx.printf("No ACL to copy from %s [%s] for %s [%s](%s)", source.getType().getName(),
				source.getObjectId().getId(), this.cmfObject.getType().name(), this.cmfObject.getLabel(),
				this.cmfObject.getId());
			return false;
		}
		final String aclDomain = source.getString(aclDomainAtt);
		final String aclName = source.getString(aclNameAtt);
		return applyAcl(sysObj, aclDomain, aclName, ctx);
	}

	protected final boolean copyAcl(IDfSysObject source, T sysObj, DctmImportContext ctx)
		throws DfException, ImportException {
		return applyAcl(sysObj, source.getACLDomain(), source.getACLName(), ctx);
	}

	protected final boolean applyAcl(T sysObj, String aclDomain, String aclName, DctmImportContext ctx)
		throws DfException, ImportException {
		final String dql = String.format("select r_object_id from dm_acl where owner_name = %s and object_name = %s",
			DfcUtils.quoteString(aclDomain), DfcUtils.quoteString(aclName));
		IDfSession session = ctx.getSession();
		final IDfId aclId;
		try (DfcQuery query = new DfcQuery(session, dql, DfcQuery.Type.DF_READ_QUERY)) {
			if (!query.hasNext()) {
				// no such ACL
				String msg = String.format(
					"Failed to find the ACL [domain=%s, name=%s] for %s [%s](%s) - the target ACL couldn't be found",
					aclDomain, aclName, this.cmfObject.getType().name(), this.cmfObject.getLabel(),
					this.cmfObject.getId());
				if (ctx.isSupported(CmfObject.Archetype.ACL)) { throw new ImportException(msg); }
				this.log.warn(msg);
				return false;
			}
			aclId = query.next().getId(DctmAttributes.R_OBJECT_ID);
		}

		ctx.printf("Applying ACL [%s::%s](%s) to %s [%s](%s)", aclDomain, aclName, aclId.getId(),
			this.cmfObject.getType().name(), this.cmfObject.getLabel(), this.cmfObject.getId());

		sysObj.setACLDomain(aclDomain);
		sysObj.setACLName(aclName);

		/*
		IDfACL acl = null;
		
		acl = session.getACL(aclDomain, aclName);
		sysObj.setACL(acl);
		
		acl = IDfACL.class.cast(session.getObject(aclId));
		sysObj.setACL(acl);
		*/
		return true;
	}

	protected boolean restoreInheritedAcl(T sysObj, DctmImportContext ctx) throws DfException, ImportException {
		CmfProperty<IDfValue> prop = this.cmfObject.getProperty(IntermediateProperty.ACL_INHERITANCE);
		if ((prop == null) || !prop.hasValues()) { return false; }
		String inheritanceSpec = prop.getValue().asString();
		Matcher m = DctmImportSysObject.ACL_INHERITANCE_PARSER.matcher(inheritanceSpec);
		if (!m.matches()) {
			this.log.warn("Invalid inheritance specification [{}] for {} [{}]({})", inheritanceSpec,
				this.cmfObject.getType().name(), this.cmfObject.getLabel(), this.cmfObject.getId());
			return false;
		}

		// Format will be inheritanceType[reference] where inheritanceType is NONE, TYPE, USER or
		// FOLDER, and reference is either empty (NONE), the type ID, user ID, or folder ID
		String type = m.group(1);
		String reference = m.group(2);

		if ("NONE".equalsIgnoreCase(type)) {
			// No ACL inheritance...just return and let the normal code apply
			return false;
		}

		IDfPersistentObject aclSource = null;
		final IDfSession session = ctx.getSession();

		if ("FOLDER".equalsIgnoreCase(type)) {
			Mapping map = ctx.getValueMapper().getTargetMapping(CmfObject.Archetype.FOLDER, DctmAttributes.R_OBJECT_ID,
				reference);
			if (map == null) {
				String msg = String.format(
					"Can't inherit an ACL from a parent folder for %s [%s](%s) - the source parent ID [%s] couldn't be mapped to a target object",
					this.cmfObject.getType().name(), this.cmfObject.getLabel(), this.cmfObject.getId(), reference);
				// No mapping...parent hasn't been mapped
				if (ctx.isSupported(CmfObject.Archetype.ACL)) { throw new ImportException(msg); }
				this.log.warn(msg);
				return false;
			}

			final IDfFolder parent;
			try {
				parent = session.getFolderBySpecification(map.getTargetValue());
			} catch (DfObjectNotFoundException e) {
				String msg = String.format(
					"Can't inherit an ACL from a parent folder for %s [%s](%s) - the parent with ID [%s] doesn't exist",
					this.cmfObject.getType().name(), this.cmfObject.getLabel(), this.cmfObject.getId(),
					map.getTargetValue());
				if (ctx.isSupported(CmfObject.Archetype.ACL)) { throw new ImportException(msg); }
				this.log.warn(msg);
				return false;
			}

			final String basePath = (parent.getFolderPathCount() > 0 ? parent.getFolderPath(0)
				: String.format("(no-parent)/%s", parent.getObjectName()));
			ctx.printf("Inheriting ACL from folder [%s](%s) for %s [%s](%s)", basePath, parent.getObjectId().getId(),
				this.cmfObject.getType().name(), this.cmfObject.getLabel(), this.cmfObject.getId());
			aclSource = parent;
		}

		if ("TYPE".equalsIgnoreCase(type)) {
			// ACL inherited from the object's type...
			IDfType t = session.getType(reference);
			if (t == null) {
				String msg = String.format(
					"Can't inherit an ACL from type [%s] for %s [%s](%s) - the type doesn't exist", reference,
					this.cmfObject.getType().name(), this.cmfObject.getLabel(), this.cmfObject.getId());
				if (ctx.isSupported(CmfObject.Archetype.ACL)) { throw new ImportException(msg); }
				this.log.warn(msg);
				return false;
			}

			IDfPersistentObject typeInfo = session.getObjectByQualification(
				String.format("dmi_type_info where r_type_id = %s", DfcUtils.quoteString(t.getObjectId().getId())));
			if (typeInfo == null) {
				this.log.warn("Can't inherit an ACL from type [{}] for {} [{}]({}) - couldn't locate the type's info",
					type, t.getName(), this.cmfObject.getType().name(), this.cmfObject.getLabel(),
					this.cmfObject.getId());
				return false;
			}

			aclSource = typeInfo;
			ctx.printf("Inheriting ACL from type [%s] for %s [%s](%s)", t.getName(), this.cmfObject.getType().name(),
				this.cmfObject.getLabel(), this.cmfObject.getId());
		}

		if ("USER".equalsIgnoreCase(type)) {
			// ACL inherited from the object's owner...
			final String user = DctmMappingUtils.resolveMappableUser(session, reference);
			IDfUser u = session.getUser(user);
			if (u == null) {
				String msg = String.format(
					"Can't inherit an ACL from user [%s] for %s [%s](%s) - the user doesn't exist (mapped to [%s])",
					reference, this.cmfObject.getType().name(), this.cmfObject.getLabel(), this.cmfObject.getId(),
					user);
				if (ctx.isSupported(CmfObject.Archetype.ACL)) { throw new ImportException(msg); }
				this.log.warn(msg);
				return false;
			}

			aclSource = u;
			ctx.printf("Inheriting ACL from user [%s] for %s [%s](%s)", u.getUserName(),
				this.cmfObject.getType().name(), this.cmfObject.getLabel(), this.cmfObject.getId());
		}

		if (aclSource == null) {
			this.log.warn("Unsupported inheritance specification [{}] for {} [{}]({})", inheritanceSpec,
				this.cmfObject.getType().name(), this.cmfObject.getLabel(), this.cmfObject.getId());
			return false;
		}

		copyAcl(aclSource, sysObj, ctx);
		return true;
	}

	protected void restoreAcl(T sysObject, boolean newObject, DctmImportContext ctx)
		throws DfException, ImportException {
		final IDfSession session = ctx.getSession();

		// First, see if there's ACL inheritance to be applied...
		if (restoreInheritedAcl(sysObject, ctx)) { return; }

		ctx.printf("ACL for %s [%s](%s) was not inherited, applying it directly from the source value",
			this.cmfObject.getType(), this.cmfObject.getLabel(), sysObject.getObjectId().getId());

		CmfAttribute<IDfValue> aclDomainAtt = this.cmfObject.getAttribute(DctmAttributes.ACL_DOMAIN);
		CmfAttribute<IDfValue> aclNameAtt = this.cmfObject.getAttribute(DctmAttributes.ACL_NAME);

		String domain = null;
		String name = null;
		String msg = null;
		if ((aclDomainAtt != null) && aclDomainAtt.hasValues() && (aclNameAtt != null) && aclNameAtt.hasValues()) {
			// New format - domain+name info
			domain = DctmMappingUtils.resolveMappableUser(session, aclDomainAtt.getValue().asString());
			name = aclNameAtt.getValue().asString();
		} else {
			// No ACL attributes? Ok...find the ACL_ID property - if it doesn't exist, we have no
			// ACL to restore...
			CmfProperty<IDfValue> aclIdProp = this.cmfObject.getProperty(IntermediateProperty.ACL_ID);
			if ((aclIdProp != null) && aclIdProp.hasValues()) {
				// old format - acl ID
				String aclId = aclIdProp.getValue().asString();
				// Find the mapped ACL
				Mapping m = ctx.getValueMapper().getTargetMapping(CmfObject.Archetype.ACL, DctmAttributes.R_OBJECT_ID,
					aclId);
				if (m != null) {
					final String dql = String.format(
						"select owner_name, object_name from dm_acl where r_object_id = %s",
						DfcUtils.quoteString(m.getTargetValue()));
					try (DfcQuery query = new DfcQuery(session, dql, DfcQuery.Type.DF_READ_QUERY)) {
						if (query.hasNext()) {
							IDfTypedObject c = query.next();
							domain = c.getString(DctmAttributes.OWNER_NAME);
							name = c.getString(DctmAttributes.OBJECT_NAME);
						} else {
							// no such ACL
							msg = String.format(
								"Failed to find the ACL [%s] for %s [%s](%s) - the ACL had a mapping (to %s), but the target ACL couldn't be found",
								aclId, this.cmfObject.getType().name(), this.cmfObject.getLabel(),
								sysObject.getObjectId().getId(), m.getTargetValue());
						}
					}
				} else {
					msg = String.format("Failed to find the ACL [%s] for %s [%s](%s) - no mapping was found", aclId,
						this.cmfObject.getType(), this.cmfObject.getLabel(), sysObject.getObjectId().getId());
				}
			}
		}

		if (msg != null) {
			if (ctx.isSupported(CmfObject.Archetype.ACL)) { throw new ImportException(msg); }
			this.log.warn(msg);
			return;
		}
		applyAcl(sysObject, domain, name, ctx);
	}

	@Override
	protected final void finalizeConstruction(T object, boolean newObject, DctmImportContext context)
		throws DfException, ImportException {
		super.finalizeConstruction(object, newObject, context);
		doFinalizeConstruction(object, newObject, context);
		// Now, link to the parent folders
		linkToParents(object, context);
		// We only try to restore ACLs if it's a new object, or if ACL support is enabled.
		if (!isReference()) {
			if (context.isSupported(CmfObject.Archetype.ACL)) {
				// If ACL processing is enabled, go full tilt...
				restoreAcl(object, newObject, context);
			}
			// TODO: This may need enabling...
			/*
			else if (newObject) {
				// If ACL processing is disabled, but this is a new object, then
				// we simply try to restore ACL inheritance as applicable
				restoreInheritedAcl(object, context);
			}
			*/
		}
	}

	protected void doFinalizeConstruction(T object, boolean newObject, DctmImportContext context)
		throws DfException, ImportException {

	}

	@Override
	protected boolean cleanupAfterSave(T object, boolean newObject, DctmImportContext context)
		throws DfException, ImportException {
		boolean ret = restoreMutability(object);
		ret |= (this.existingTemporaryPermission != null) && this.existingTemporaryPermission.revoke(object);
		return ret;
	}

	@Override
	protected IDfId persistChanges(T sysObject, DctmImportContext context) throws DfException, ImportException {
		if (!sysObject.isCheckedOut()) { return super.persistChanges(sysObject, context); }
		IDfId newId = persistNewVersion(sysObject, null, context);
		updateIdAndHistoryMappings(context, sysObject, newId);
		return newId;
	}

	protected IDfId persistNewVersion(T sysObject, String versionLabel, DctmImportContext context) throws DfException {
		String vl = (versionLabel != null ? versionLabel
			: DctmImportTools.concatenateStrings(this.cmfObject.getAttribute(DctmAttributes.R_VERSION_LABEL), ','));
		IDfValue branchMarker = context.getValue(DctmImportSysObject.BRANCH_MARKER);
		final IDfId newId;
		final String action;
		if ((branchMarker == null) || !branchMarker.asBoolean()) {
			action = "Checked in";
			if (StringUtils.countMatches(vl, ".") > 1) {
				// Don't specify version labels for branch commits?
				vl = null;
			}
			newId = sysObject.checkin(false, vl);
		} else {
			action = "Branched";
			newId = sysObject.getObjectId();
			sysObject.save();
		}
		this.log.info("{} {} [{}]({}) to CMS as version [{}] (newId={})", action, this.cmfObject.getType(),
			this.cmfObject.getLabel(), this.cmfObject.getId(), vl, newId.getId());
		return newId;
	}

	@Override
	protected void finalizeOperation(T sysObject) throws DfException, ImportException {
	}

	@Override
	protected String generateSystemAttributesSQL(CmfObject<IDfValue> stored, IDfPersistentObject sysObject,
		DctmImportContext ctx) throws DfException {
		if (!IDfSysObject.class.isInstance(sysObject)) {
			// how, exactly, did we get here?
			return null;
		}

		final IDfSession session = ctx.getSession();

		// prepare sql to be executed
		// Important: REPLACE QUOTES!!

		CmfAttribute<IDfValue> modifyDateAtt = stored.getAttribute(DctmAttributes.R_MODIFY_DATE);
		final String modifyDate;
		if (modifyDateAtt != null) {
			modifyDate = DfcUtils.generateSqlDateClause(modifyDateAtt.getValue().asTime().getDate(), session);
		} else {
			modifyDate = "r_modify_date";
		}

		CmfAttribute<IDfValue> modifierNameAtt = stored.getAttribute(DctmAttributes.R_MODIFIER);
		String modifierName = "";
		if (modifierNameAtt != null) {
			modifierName = modifierNameAtt.getValue().asString();
			try {
				IDfUser u = DctmImportUser.locateExistingUser(ctx, modifierName);
				if (u == null) {
					modifierName = "";
				}
			} catch (MultipleUserMatchesException e) {
				modifierName = "";
			}
		}
		if (modifierName.length() == 0) {
			modifierName = "${owner_name}";
		}
		modifierName = DfcUtils.quoteStringForSql(DctmMappingUtils.resolveMappableUser(session, modifierName));

		CmfAttribute<IDfValue> creationDateAtt = stored.getAttribute(DctmAttributes.R_CREATION_DATE);
		final String creationDate;
		if (creationDateAtt != null) {
			creationDate = DfcUtils.generateSqlDateClause(creationDateAtt.getValue().asTime().getDate(), session);
		} else {
			creationDate = "r_creation_date";
		}

		CmfAttribute<IDfValue> creatorNameAtt = stored.getAttribute(DctmAttributes.R_CREATOR_NAME);
		String creatorName = "";
		if (creatorNameAtt != null) {
			try {
				IDfUser u = DctmImportUser.locateExistingUser(ctx, creatorNameAtt.getValue().asString());
				if (u != null) {
					creatorName = u.getUserName();
				}
			} catch (ImportException e) {
				// Multiple matches...so don't do anything
			}
		}
		if (creatorName.length() == 0) {
			creatorName = "${owner_name}";
		}
		creatorName = DfcUtils.quoteStringForSql(DctmMappingUtils.resolveMappableUser(session, creatorName));

		CmfAttribute<IDfValue> deletedAtt = stored.getAttribute(DctmAttributes.I_IS_DELETED);
		final boolean deletedFlag = (deletedAtt != null) && deletedAtt.getValue().asBoolean();

		String sql = "" //
			+ "UPDATE dm_sysobject_s SET " //
			+ "       r_modify_date = %s, " //
			+ "       r_modifier = %s, " //
			+ "       r_creation_date = %s, " //
			+ "       r_creator_name = %s, " //
			+ "       i_is_deleted = %d " //
			+ "       %s " //
			+ " WHERE r_object_id = %s";
		String vstampFlag = "";
		// TODO: For now we don't touch the i_vstamp b/c we don't think it necessary
		// (Setting.SKIP_VSTAMP.getBoolean() ? "" : String.format(", i_vstamp = %d",
		// dctmObj.getIntSingleAttrValue(CmsAttributes.I_VSTAMP)));
		return String.format(sql, modifyDate, modifierName, creationDate, creatorName, (deletedFlag ? 1 : 0),
			vstampFlag, DfcUtils.quoteStringForSql(sysObject.getObjectId().getId()));
	}

	/**
	 * Removes all links of an object in CMS.
	 *
	 * @param object
	 *            the DFC sysObject who is being unlinked
	 * @throws DfException
	 *             Signals that Dctm Server error has occurred.
	 */
	protected final Set<String> removeAllLinks(IDfSysObject object) throws DfException {
		Set<String> ret = new HashSet<>();
		int folderIdCount = object.getFolderIdCount();
		for (int i = 0; i < folderIdCount; i++) {
			// We have to do it backwards, instead of forwards, because it's
			// faster. Otherwise, when we remove an "earlier" element, the ones
			// down the list have to be shifted "forward" by DCTM, and that takes
			// time, which we try to avoid by being smart...
			String id = object.getFolderId(folderIdCount - i - 1).getId();
			if (!ret.contains(id)) {
				ret.add(id);
			}
			object.unlink(id);
		}
		return ret;
	}

	protected boolean isReference() {
		// Prefer the Documentum attribute over the property...
		CmfProperty<IDfValue> reference = this.cmfObject.getAttribute(DctmAttributes.I_IS_REFERENCE);
		if (reference == null) {
			// No attribute? Possibly doesn't come from Documentum...go for the property
			reference = this.cmfObject.getProperty(IntermediateProperty.IS_REFERENCE);
		}
		return ((reference != null) && reference.hasValues() && reference.getValue().asBoolean());
	}

	protected boolean isDfReference(T object) throws DfException {
		return object.isReference();
	}

	protected T newReference(DctmImportContext context) throws DfException, ImportException {
		IDfSysObject target = null;
		final IDfSession session = context.getSession();
		IDfValue bindingCondition = this.cmfObject.getProperty(DctmAttributes.BINDING_CONDITION).getValue();
		IDfValue bindingLabel = this.cmfObject.getProperty(DctmAttributes.BINDING_LABEL).getValue();
		IDfValue referenceById = this.cmfObject.getProperty(DctmAttributes.REFERENCE_BY_ID).getValue();

		// First, try to map the ID...
		Mapping m = context.getValueMapper().getTargetMapping(this.cmfObject.getType(), DctmAttributes.R_OBJECT_ID,
			referenceById.asString());
		if (m != null) {
			referenceById = DfValueFactory.ofId(m.getTargetValue());
		}

		target = IDfSysObject.class.cast(session.getObject(referenceById.asId()));
		if (!IDfSysObject.class.isInstance(target)) {
			throw new ImportException(String.format("Reference [%s] target object [%s] is not an IDfSysObject instance",
				this.cmfObject.getLabel(), referenceById.asString()));
		}

		IDfSysObject targetSysObj = IDfSysObject.class.cast(target);
		IDfId mainFolderId = getMappedParentId(context);
		if (mainFolderId == null) {
			mainFolderId = this.cmfObject.getProperty(IntermediateProperty.PARENT_ID).getValue().asId();
			throw new ImportException(
				String.format("Reference [%s] mapping for its parent folder [%s->???] could not be found",
					this.cmfObject.getLabel(), mainFolderId.getId()));
		}
		final IDfId newId = targetSysObj.addReference(mainFolderId, bindingCondition.asString(),
			bindingLabel.asString());

		final IDfReference ref;
		try (DfcQuery query = new DfcQuery(session,
			String.format("select r_object_id from dm_reference_s where r_mirror_object_id = %s",
				DfcUtils.quoteString(newId.getId())))) {
			if (!query.hasNext()) {
				// ERROR!
				throw new ImportException(String.format("Reference [%s] could not be found with its new ID [%s]",
					this.cmfObject.getLabel(), newId.getId()));
			}
			ref = IDfReference.class.cast(session.getObject(query.next().getId("r_object_id")));
		}
		CmfProperty<IDfValue> p = null;
		p = this.cmfObject.getProperty(DctmAttributes.REFRESH_INTERVAL);
		if ((p != null) && p.hasValues()) {
			ref.setRefreshInterval(p.getValue().asInteger());
		}
		/*
		p = this.cmfObject.getProperty(DctmAttributes.REFERENCE_DB_NAME);
		if ((p != null) && p.hasValues()) {
			ref.setReferenceDbName(p.getValue().asString());
		}
		p = this.cmfObject.getProperty(DctmAttributes.LOCAL_FOLDER_LINK);
		if ((p != null) && p.hasValues()) {
			ref.setReferenceDbName(p.getValue().asString());
		}
		p = this.cmfObject.getProperty(DctmAttributes.REFERENCE_BY_NAME);
		if ((p != null) && p.hasValues()) {
			ref.setReferenceDbName(p.getValue().asString());
		}
		*/
		ref.save();
		return castObject(session.getObject(newId));
	}

	@Override
	protected boolean isShortConstructionCycle() {
		// References require a modified algorithm...
		return isReference();
	}

	protected Collection<IDfValue> getTargetPaths() throws DfException, ImportException {
		CmfProperty<IDfValue> p = this.cmfObject.getProperty(IntermediateProperty.PATH);
		if ((p == null) || (p.getValueCount() == 0)) {
			throw new ImportException(String.format("No target paths specified for [%s](%s)", this.cmfObject.getLabel(),
				this.cmfObject.getId()));
		}
		return p.getValues();
	}

	private boolean convertObjectType(IDfSession session, T obj, IDfType source, IDfType target)
		throws ImportException, DfException {
		// Validate that the types *must* be related
		if (!target.isTypeOf(source.getName()) && !source.isTypeOf(target.getName())) {
			if (this.log.isDebugEnabled()) {
				this.log.warn("Cannot convert the type from {} to {} for {} ({}) because they're not related",
					source.getName(), target.getName(), obj.getObjectId(), this.cmfObject.getDescription());
			}
			return false;
		}
		// Ok so they're related ... maybe try to modify the type into the subtype?
		String changeTypeDQL = String.format("CHANGE OBJECTS %s (ALL) TO %s WHERE i_chronicle_id = ID(%s)",
			source.getName(), target.getName(), obj.getChronicleId().getId());
		DfcQuery.run(session, changeTypeDQL);
		return true;
	}

	protected T locateExistingByPath(DctmImportContext ctx) throws ImportException, DfException {
		final IDfSession session = ctx.getSession();
		final String objectName = this.cmfObject.getAttribute(DctmAttributes.OBJECT_NAME).getValue().asString();

		IDfType type = DctmTranslator.translateType(ctx, this.cmfObject);
		if (type == null) {
			throw new ImportException(String.format("Unsupported subtype [%s] and object type [%s] in object [%s](%s)",
				this.cmfObject.getSubtype(), this.cmfObject.getType(), this.cmfObject.getLabel(),
				this.cmfObject.getId()));
		}

		final boolean attemptTypeChange = this.factory.isAdjustTypesEnabled();
		final String dqlBase = String.format("%s (ALL) where object_name = %%s and folder(%%s)", type.getName());

		final boolean seeksReference = isReference();
		String existingPath = null;
		T existing = null;
		final Class<T> dfClass = getObjectClass();
		Collection<IDfValue> targetPaths = getTargetPaths();
		if (this.log.isDebugEnabled()) {
			this.log.debug("Found {} target paths for {}", targetPaths.size(), this.cmfObject.getDescription());
		}
		for (IDfValue p : getTargetPaths()) {
			final String targetPath = ctx.getTargetPath(p.asString());
			final String dql = String.format(dqlBase, DfcUtils.quoteString(objectName),
				DfcUtils.quoteString(targetPath));
			final String currentPath = String.format("%s/%s", targetPath, objectName);
			this.log.debug("Searching for an object using qualification [{}]", dql);
			IDfPersistentObject current = session.getObjectByQualification(dql);
			if (current == null) {
				if (this.log.isDebugEnabled()) {
					this.log.debug("Did not find a matching object for {} using DQL qualification: [{}]",
						this.cmfObject.getDescription(), dql);
					this.log.debug("Searching for a matching object for {} using the path [{}]",
						this.cmfObject.getDescription(), currentPath);
				}

				// No match, try by path?
				current = session.getObjectByPath(currentPath);
				if (current == null) {
					if (this.log.isDebugEnabled()) {
						this.log.debug("Did not find a matching object for {} using the path [{}]",
							this.cmfObject.getDescription(), currentPath);
					}
					continue;
				}
			}

			if (this.log.isDebugEnabled()) {
				this.log.debug("Object found with id [{}] as a match for {}", current.getObjectId(),
					this.cmfObject.getDescription());
			}

			// Verify hierarchy ... is the existing object's type a subtype of the incoming type?
			final boolean currentIsSubType = current.getType().isTypeOf(type.getName());
			final boolean currentIsSuperType = type.isTypeOf(current.getType().getName());

			boolean typeIsCompatible = false;
			if (currentIsSubType != currentIsSuperType) {
				typeIsCompatible = (currentIsSubType || attemptTypeChange);
				if (attemptTypeChange) {
					if (this.log.isDebugEnabled()) {
						this.log.warn(
							"Type mismatch for object [{}] (for {}): expected {} but was {} ... will attempt to change it",
							current.getObjectId(), this.cmfObject.getDescription(), current.getType().getName(),
							type.getName());
					}
					typeIsCompatible = convertObjectType(session, existing, current.getType(), type);
					current.fetch(null); // Re-fetch the re-typed object
				} else if (currentIsSuperType) {
					// if we're not doing type changes, and the current is a supertype,
					// then we must fail the operation, for safety
					typeIsCompatible = false;
				}
			} else {
				// If the hierarchy flags are the same, then we're only OK
				// if they're both true. Otherwise, they're both false and that's
				// a problem because the types aren't related at all
				typeIsCompatible = (currentIsSubType && currentIsSuperType);
			}

			if (!typeIsCompatible) {
				throw new ImportException(String.format(
					"Found an incompatible object in one of the %s [%s] %s's intended paths: [%s] = [%s:%s]",
					this.cmfObject.getType(), this.cmfObject.getLabel(), this.cmfObject.getSubtype(), currentPath,
					current.getType().getName(), current.getObjectId().getId()));
			}

			T currentObj = dfClass.cast(current);
			if (isDfReference(currentObj) != seeksReference) {
				// The target document's reference flag is different from ours...problem!
				throw new ImportException(String.format(
					"Reference flag mismatch between objects. The [%s] %s collides with a %sreference at [%s] (%s:%s)",
					this.cmfObject.getLabel(), (seeksReference ? "dm_reference" : this.cmfObject.getSubtype()),
					(seeksReference ? "non-" : ""), currentPath, current.getType().getName(),
					current.getObjectId().getId()));
			}

			if (existing == null) {
				// First match, keep track of it
				existing = currentObj;
				existingPath = currentPath;
				continue;
			}

			// Second match, is it the same as the first?
			if (Objects.equals(existing.getObjectId().getId(), current.getObjectId().getId())) {
				// Same as the first - we have no issue here
				continue;
			}

			// Not the same, this is a problem
			throw new ImportException(String.format(
				"Found two different objects matching the [%s] paths: [%s@%s] and [%s@%s]", this.cmfObject.getLabel(),
				existing.getObjectId().getId(), existingPath, current.getObjectId().getId(), currentPath));
		}
		return existing;
	}

	@Override
	protected T locateInCms(DctmImportContext ctx) throws ImportException, DfException {
		return locateExistingByPath(ctx);
	}

	protected IDfId getMappedParentId(DctmImportContext context) throws DfException, ImportException {
		return getMappedParentId(context, 0);
	}

	protected IDfId getMappedParentId(DctmImportContext context, int pos) throws DfException, ImportException {
		final IDfSession session = context.getSession();
		CmfProperty<IDfValue> parents = this.cmfObject.getProperty(IntermediateProperty.PARENT_ID);
		IDfId mainFolderId = parents.getValue(pos).asId();
		if (mainFolderId.isNull()) {
			// This is only valid if pos is 0, and it's the only parent value, and there's only one
			// path value. If it's used under any other circumstance, it's an error.
			CmfProperty<IDfValue> paths = this.cmfObject.getProperty(IntermediateProperty.PATH);
			if ((pos == 0) && (parents.getValueCount() == 1) && (paths.getValueCount() == 1)) {
				// This is a "fixup" from the path repairs, so we look up by path
				String path = context.getTargetPath(paths.getValue().asString());
				IDfFolder f = session.getFolderByPath(path);
				if (f != null) { return f.getObjectId(); }
				this.log.warn("Fixup path [{}] for {} was not found", path, this.cmfObject.getDescription());
			}
		}
		Mapping m = context.getValueMapper().getTargetMapping(DctmObjectType.FOLDER.getStoredObjectType(),
			DctmAttributes.R_OBJECT_ID, mainFolderId.getId());
		if (m != null) { return new DfId(m.getTargetValue()); }
		return null;
	}

	protected List<String> getProspectiveParents(DctmImportContext context) throws DfException, ImportException {
		CmfProperty<IDfValue> parents = this.cmfObject.getProperty(IntermediateProperty.PARENT_ID);
		List<String> newParents = new ArrayList<>();
		if ((parents == null) || (parents.getValueCount() == 0)) {
			// This might be a cabinet import, so we try to find the target folder
			String rootPath = context.getTargetPath("/");
			IDfFolder root = context.getSession().getFolderByPath(rootPath);
			if (root != null) {
				newParents.add(root.getObjectId().getId());
			}
		} else {
			for (int i = 0; i < parents.getValueCount(); i++) {
				IDfId parentId = getMappedParentId(context, i);
				if (parentId == null) {
					continue;
				}
				newParents.add(parentId.toString());
			}
		}
		return newParents;
	}

	protected final boolean linkToParents(T sysObject, DctmImportContext ctx) throws DfException, ImportException {
		final IDfSession session = ctx.getSession();

		// First, get the list of the current parents, and reverse its order.
		// The reverse order will accelerate the unlinking process later on
		final int cpcount = sysObject.getFolderIdCount();
		LinkedList<String> oldParents = new LinkedList<>();
		for (int i = 0; i < cpcount; i++) {
			oldParents.addLast(sysObject.getFolderId(i).getId());
		}

		List<String> newParents = getProspectiveParents(ctx);

		// This ensures that we acquire ALL locks in the correct lowest-id-first order,
		// since parentLinkActions will be a sorted tree in that order.
		Set<String> lockingOrder = new TreeSet<>();
		lockingOrder.addAll(oldParents);
		lockingOrder.addAll(newParents);

		Set<String> common = new HashSet<>(oldParents);
		common.retainAll(newParents);

		// If the "common" set is the same size as the "lockingOrder" set,
		// then they forcibly have the same elements, so we don't have to do
		// any linking/unlinking
		if (common.size() == lockingOrder.size()) {
			// For safety...
			this.parentLinkActions = Collections.emptyList();
			return false;
		}

		// Ok...so first things first: lock the objects, and pull them in for
		// processing
		Map<String, IDfSysObject> parentCache = new HashMap<>(lockingOrder.size());
		for (String parentId : lockingOrder) {
			// We HAVE to lock everything in the correct order prior to modification
			final IDfId id = new DfId(parentId);
			session.flushObject(id);
			final IDfSysObject parent = IDfSysObject.class.cast(session.getObject(id));
			DfcUtils.lockObject(this.log, parent);
			parentCache.put(parentId, parent);
		}

		// Ok...so now we have the parents locked, in the right order, and the object
		// for the parent is cached...proceed with the unlink first
		this.parentLinkActions = new ArrayList<>(lockingOrder.size());
		for (String parentId : oldParents) {
			IDfSysObject parent = parentCache.get(parentId);
			ParentFolderAction action = new ParentFolderAction(ctx.getSession(), parent, false);
			this.log.debug("Applying {}", action);
			action.apply(sysObject);
			this.log.debug("Applied {}", action);
			this.parentLinkActions.add(action);
		}

		for (String parentId : newParents) {
			IDfSysObject parent = parentCache.get(parentId);
			ParentFolderAction action = new ParentFolderAction(ctx.getSession(), parent, true);
			this.log.debug("Applying {}", action);
			action.apply(sysObject);
			this.log.debug("Applied {}", action);
			this.parentLinkActions.add(action);
		}

		return true;
	}

	protected final void cleanUpParents(IDfSession session) throws DfException, ImportException {
		if (this.parentLinkActions != null) {
			for (ParentFolderAction action : this.parentLinkActions) {
				action.cleanUp();
			}
			this.parentLinkActions = null;
		}
	}

	@Override
	protected T newObject(DctmImportContext ctx) throws DfException, ImportException {
		T newObj = super.newObject(ctx);
		setOwnerGroupACLData(newObj, ctx);
		if (ctx.isSupported(CmfObject.Archetype.DATASTORE)) {
			CmfAttribute<IDfValue> att = this.cmfObject.getAttribute(DctmAttributes.A_STORAGE_TYPE);
			String dataStore = "";
			if ((att != null) && att.hasValues()) {
				dataStore = att.getValue().asString();
			}
			if (!StringUtils.isBlank(dataStore) && !Objects.equals(newObj.getStorageType(), dataStore)) {
				newObj.setStorageType(dataStore);
			}
		}
		return newObj;
	}

	protected void setOwnerGroupACLData(T sysObject, DctmImportContext ctx) throws ImportException, DfException {
		// Set the owner and group
		final IDfSession session = ctx.getSession();
		CmfAttribute<IDfValue> att = this.cmfObject.getAttribute(DctmAttributes.OWNER_NAME);
		if (att != null) {
			final String actualUser = DctmMappingUtils.resolveMappableUser(session, att.getValue().asString());
			try {
				IDfUser u = DctmImportUser.locateExistingUser(ctx, actualUser);
				if (u != null) {
					sysObject.setOwnerName(u.getUserName());
				} else {
					String msg = String.format(
						"Failed to set the owner for %s [%s](%s) to user [%s] - the user wasn't found - probably didn't need to be copied over",
						this.cmfObject.getType(), this.cmfObject.getLabel(), sysObject.getObjectId().getId(),
						actualUser);
					if (ctx.isSupported(CmfObject.Archetype.USER)) { throw new ImportException(msg); }
					this.log.warn(msg);
				}
			} catch (MultipleUserMatchesException e) {
				String msg = String.format("Failed to set the owner for %s [%s](%s) to user [%s] - %s",
					this.cmfObject.getType(), this.cmfObject.getLabel(), sysObject.getObjectId().getId(), actualUser,
					e.getMessage());
				if (ctx.isSupported(CmfObject.Archetype.USER)) { throw new ImportException(msg); }
				this.log.warn(msg);
			}
		}

		att = this.cmfObject.getAttribute(DctmAttributes.GROUP_NAME);
		if (att != null) {
			String group = att.getValue().asString();
			IDfGroup g = session.getGroup(group);
			if (g != null) {
				sysObject.setGroupName(g.getGroupName());
			} else {
				String msg = String.format(
					"Failed to set the group for %s [%s](%s) to group [%s] - the group wasn't found - probably didn't need to be copied over",
					this.cmfObject.getType(), this.cmfObject.getLabel(), sysObject.getObjectId().getId(), group);
				if (ctx.isSupported(CmfObject.Archetype.GROUP)) { throw new ImportException(msg); }
				this.log.warn(msg);
			}
		}
	}

	@Override
	protected ImportOutcome doImportObject(DctmImportContext context) throws DfException, ImportException {
		// First things first: fix the parent paths in the incoming object
		CmfProperty<IDfValue> paths = this.cmfObject.getProperty(IntermediateProperty.PATH);
		if (paths == null) {
			paths = new CmfProperty<>(IntermediateProperty.PATH, CmfValue.Type.STRING, true);
			this.cmfObject.setProperty(paths);
			this.log.warn("Added the {} property for [{}]({}) (missing at the source)", IntermediateProperty.PATH,
				this.cmfObject.getLabel(), this.cmfObject.getId());
		}

		CmfProperty<IDfValue> parents = this.cmfObject.getProperty(IntermediateProperty.PARENT_ID);
		if (parents == null) {
			parents = new CmfProperty<>(IntermediateProperty.PARENT_ID, CmfValue.Type.ID, true);
			this.cmfObject.setProperty(parents);
			this.log.warn("Added the {} property for [{}]({}) (missing at the source)", PropertyIds.PARENT_ID,
				this.cmfObject.getLabel(), this.cmfObject.getId());
		}

		return super.doImportObject(context);
	}

	@Override
	protected boolean isSameObject(T object, DctmImportContext ctx) throws DfException, ImportException {
		if (!super.isSameObject(object, ctx)) { return false; }

		// Same dates?
		if (!isAttributeEquals(object, DctmAttributes.R_MODIFY_DATE)) { return false; }

		// Same ACL?
		if (!isAttributeEquals(object, DctmAttributes.ACL_NAME)) { return false; }
		CmfAttribute<IDfValue> att = this.cmfObject.getAttribute(DctmAttributes.ACL_DOMAIN);
		if ((att == null) || !att.hasValues()) { return false; }
		String aclDomain = DctmMappingUtils.resolveMappableUser(ctx.getSession(), att.getValue().asString());
		if (!StringUtils.equals(aclDomain, object.getACLDomain())) { return false; }

		// Same owner?
		if (ctx.isSupported(CmfObject.Archetype.USER)) {
			if (!isAttributeEquals(object, DctmAttributes.OWNER_NAME)) { return false; }
		}

		// Same group?
		if (ctx.isSupported(CmfObject.Archetype.GROUP)) {
			if (!isAttributeEquals(object, DctmAttributes.GROUP_NAME)) { return false; }
		}

		// Same world access?
		if (!isAttributeEquals(object, DctmAttributes.WORLD_PERMIT)) { return false; }

		return true;
	}

}