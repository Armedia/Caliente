/**
 *
 */

package com.armedia.caliente.engine.documentum.importer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.documentum.DctmAttributes;
import com.armedia.caliente.engine.documentum.DctmMappingUtils;
import com.armedia.caliente.engine.documentum.DctmObjectType;
import com.armedia.caliente.engine.documentum.DctmTranslator;
import com.armedia.caliente.engine.documentum.DfUtils;
import com.armedia.caliente.engine.documentum.DfValueFactory;
import com.armedia.caliente.engine.documentum.common.DctmSysObject;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.engine.importer.ImportOutcome;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeMapper.Mapping;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfType;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.DfObjectNotFoundException;
import com.documentum.fc.client.DfPermit;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfGroup;
import com.documentum.fc.client.IDfPermit;
import com.documentum.fc.client.IDfPermitType;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.IDfType;
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

	protected static final class TemporaryPermission {
		private final Logger log = LoggerFactory.getLogger(getClass());

		private final String objectId;
		private final IDfPermit oldPermit;
		private final IDfPermit newPermit;
		private final Set<String> newXPermit;
		private final Set<String> autoRemove;

		public TemporaryPermission(IDfSysObject object, int newPermission, String... newXPermits) throws DfException {
			this(object, newPermission,
				(newXPermits == null ? DctmImportSysObject.NO_PERMITS : Arrays.asList(newXPermits)));
		}

		public TemporaryPermission(IDfSysObject object, int newPermission, Collection<String> newXPermits)
			throws DfException {
			this.objectId = object.getObjectId().getId();
			final String userName = object.getSession().getLoginUserName();

			// Does it have the required access permission?
			object.fetch(null);
			int oldPermission = object.getPermitEx(userName);
			if (oldPermission < newPermission) {
				if (oldPermission > 0) {
					this.oldPermit = new DfPermit();
					this.oldPermit.setAccessorName(userName);
					this.oldPermit.setPermitType(IDfPermitType.ACCESS_PERMIT);
					this.oldPermit.setPermitValue(DfUtils.decodeAccessPermission(oldPermission));
				} else {
					this.oldPermit = null;
				}
				this.newPermit = new DfPermit();
				this.newPermit.setAccessorName(userName);
				this.newPermit.setPermitType(IDfPermitType.ACCESS_PERMIT);
				this.newPermit.setPermitValue(DfUtils.decodeAccessPermission(newPermission));
			} else {
				this.oldPermit = null;
				this.newPermit = null;
			}

			Set<String> s = new HashSet<>(StrTokenizer.getCSVInstance(object.getXPermitList()).getTokenList());
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
			if (!Tools.equals(this.objectId,
				object.getObjectId().getId())) { throw new DfException(
					String.format("ERROR: Expected object with ID [%s] but got [%s] instead", this.objectId,
						object.getObjectId().getId())); }
			boolean ret = false;
			if (this.newPermit != null) {
				IDfPermit toGrant = (grant ? this.newPermit : this.oldPermit);
				IDfPermit toRevoke = (grant ? this.oldPermit : this.newPermit);
				if (toRevoke != null) {
					if (this.log.isDebugEnabled()) {
						this.log.debug(String.format("REVOKING [%s] on [%s]", toRevoke.getPermitValueString(),
							object.getObjectId()));
					}
					object.revokePermit(toRevoke);
				}
				if (toGrant != null) {
					if (this.log.isDebugEnabled()) {
						this.log.debug(String.format("GRANTING [%s] on [%s]", toGrant.getPermitValueString(),
							object.getObjectId()));
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
						this.log.debug(String.format("REVOKING AUTO-XPERM [%s] on [%s]", auto.getPermitValueString(),
							object.getObjectId()));
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
					this.log.debug(
						String.format("%s [%s] on [%s]", (grant ? "GRANTING" : "REVOKING"), p, object.getObjectId()));
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

		protected ParentFolderAction(IDfSysObject parent, boolean link) throws DfException {
			this.parent = parent;
			this.parentId = parent.getObjectId().getId();
			this.link = link;
			this.TemporaryPermission = new TemporaryPermission(parent, IDfACL.DF_PERMIT_DELETE,
				IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR);
		}

		private void ensureLocked() throws DfException {
			if (!this.locked) {
				DfUtils.lockObject(this.log, this.parent);
				this.parent.fetch(null);
				this.locked = true;
			}
		}

		protected void apply(IDfSysObject child) throws DfException {
			ensureLocked();
			if (this.TemporaryPermission.grant(this.parent)) {
				this.parent.save();
			}
			if (this.log.isDebugEnabled()) {
				this.log.debug(String.format("%sLINKING [%s] --> [%s]", this.link ? "" : "UN",
					child.getObjectId().getId(), this.parent.getObjectId().getId()));
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
			if (!Tools.equals(this.parentId, other.parentId)) { return false; }
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
				this.log.debug(String.format("Clearing frozen status from [%s](%s){%s}", this.cmfObject.getLabel(),
					this.cmfObject.getId(), newId));
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
				this.log.debug(String.format("Clearing immutable status from [%s](%s){%s}", this.cmfObject.getLabel(),
					this.cmfObject.getId(), newId));
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
				this.log.debug(String.format("Setting frozen status to [%s](%s){%s}", this.cmfObject.getLabel(),
					this.cmfObject.getId(), newId));
			}
			// TODO: assembly support?
			// TODO: How to determine if we use false or true here? Stick to false, for now...
			sysObject.freeze(false);
			ret |= true;
		} else if (this.mustImmute && !sysObject.isImmutable()) {
			if (this.log.isDebugEnabled()) {
				this.log.debug(String.format("Setting immutability status to [%s](%s){%s}", this.cmfObject.getLabel(),
					this.cmfObject.getId(), newId));
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
			this.existingTemporaryPermission = new TemporaryPermission(sysObject, IDfACL.DF_PERMIT_DELETE);
			if (this.existingTemporaryPermission.grant(sysObject)) {
				sysObject.save();
			}
		}
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

		String actualReference = null;
		String aclDomain = null;
		String aclName = null;

		final IDfSession session = ctx.getSession();

		if ("FOLDER".equalsIgnoreCase(type)) {
			Mapping map = ctx.getAttributeMapper().getTargetMapping(CmfType.FOLDER, DctmAttributes.R_OBJECT_ID,
				reference);
			if (map == null) {
				// No mapping...parent hasn't been mapped
				throw new ImportException(String.format(
					"Can't inherit an ACL from a parent folder for %s [%s](%s) - the source parent ID [%s] couldn't be mapped to a target object",
					this.cmfObject.getType().name(), this.cmfObject.getLabel(), this.cmfObject.getId(), reference));
			}

			final IDfFolder parent;
			try {
				parent = session.getFolderBySpecification(map.getTargetValue());
			} catch (DfObjectNotFoundException e) {
				throw new ImportException(String.format(
					"Can't inherit an ACL from a parent folder for %s [%s](%s) - the parent with ID [%s] doesn't exist",
					this.cmfObject.getType().name(), this.cmfObject.getLabel(), this.cmfObject.getId(),
					map.getTargetValue()));
			}

			final IDfACL acl = parent.getACL();
			if (acl != null) {
				sysObj.setACL(acl);
				return true;
			}

			this.log.warn(
				"Can't inherit an ACL from parent folder [{}] for {} [{}]({}) - the parent has no ACL to inherit from",
				parent.getFolderPath(0), this.cmfObject.getType().name(), this.cmfObject.getLabel(),
				this.cmfObject.getId());
			return false;
		}

		if ("TYPE".equalsIgnoreCase(type)) {
			// ACL inherited from the object's type...
			IDfType t = session.getType(reference);
			if (t == null) { throw new ImportException(
				String.format("Can't inherit an ACL from type [%s] for %s [%s](%s) - the type doesn't exist", reference,
					this.cmfObject.getType().name(), this.cmfObject.getLabel(), this.cmfObject.getId())); }

			actualReference = t.getName();

			IDfPersistentObject typeInfo = session.getObjectByQualification(
				String.format("dmi_type_info where r_type_id = %s", DfUtils.quoteString(t.getObjectId().getId())));
			if (typeInfo == null) {
				this.log.warn("Can't inherit an ACL from type [{}] for {} [{}]({}) - couldn't locate the type's info",
					type, actualReference, this.cmfObject.getType().name(), this.cmfObject.getLabel(),
					this.cmfObject.getId());
				return false;
			}

			aclDomain = typeInfo.getString(DctmAttributes.ACL_DOMAIN);
			aclName = typeInfo.getString(DctmAttributes.ACL_NAME);
		}

		if ("USER".equalsIgnoreCase(type)) {
			// ACL inherited from the object's owner...
			final String user = DctmMappingUtils.resolveMappableUser(session, reference);
			IDfUser u = session.getUser(user);
			if (u == null) { throw new ImportException(String.format(
				"Can't inherit an ACL from user [%s] for %s [%s](%s) - the user doesn't exist (mapped to [%s])",
				reference, this.cmfObject.getType().name(), this.cmfObject.getLabel(), this.cmfObject.getId(), user)); }

			actualReference = u.getUserName();
			aclDomain = u.getACLDomain();
			aclName = u.getACLName();
		}

		if (actualReference == null) {
			this.log.warn("Unsupported inheritance specification [{}] for {} [{}]({})", inheritanceSpec,
				this.cmfObject.getType().name(), this.cmfObject.getLabel(), this.cmfObject.getId());
			return false;
		}

		if (StringUtils.isEmpty(aclName) || StringUtils.isEmpty(aclDomain)) { throw new ImportException(String.format(
			"The %s [%s] doesn't contain any ACL information - can't inherit an ACL for %s [%s](%s)", type,
			actualReference, this.cmfObject.getType().name(), this.cmfObject.getLabel(), this.cmfObject.getId())); }

		final IDfACL acl = session.getACL(aclDomain, aclName);
		if (acl == null) { throw new ImportException(
			String.format("The %s [%s] references a nonexistent ACL [%s::%s] - can't inherit an ACL for %s [%s](%s)",
				type, actualReference, aclDomain, aclName, this.cmfObject.getType().name(), this.cmfObject.getLabel(),
				this.cmfObject.getId())); }

		sysObj.setACL(acl);
		return true;
	}

	protected void restoreAcl(T sysObject, DctmImportContext ctx) throws DfException, ImportException {
		final IDfSession session = ctx.getSession();

		// First, see if there's ACL inheritance to be applied...
		if (restoreInheritedAcl(sysObject, ctx)) { return; }

		// First, find the ACL_ID property - if it doesn't exist, we have no ACL to restore
		CmfProperty<IDfValue> aclIdProp = this.cmfObject.getProperty(IntermediateProperty.ACL_ID);
		if ((aclIdProp != null) && aclIdProp.hasValues()) {
			IDfId aclId = aclIdProp.getValue().asId();
			if (aclId.isNull()) {
				// No acl...
				return;
			}

			// Find the mapped ACL
			Mapping m = ctx.getAttributeMapper().getTargetMapping(CmfType.ACL, DctmAttributes.R_OBJECT_ID,
				aclId.getId());
			String msg = null;
			if (m != null) {
				try {
					IDfACL acl = IDfACL.class.cast(session.getObject(new DfId(m.getTargetValue())));
					sysObject.setACL(acl);
					return;
				} catch (DfObjectNotFoundException e) {
				}

				// ACL or not, we're done here...
				msg = String.format(
					"Failed to find the ACL [%s] for %s [%s](%s) - the ACL had a mapping (to %s), but the target ACL couldn't be found",
					aclId.getId(), this.cmfObject.getType(), this.cmfObject.getLabel(), sysObject.getObjectId().getId(),
					m.getTargetValue());
			} else {
				msg = String.format("Failed to find the ACL [%s] for %s [%s](%s) - no mapping was found", aclId.getId(),
					this.cmfObject.getType(), this.cmfObject.getLabel(), sysObject.getObjectId().getId());
			}

			if (ctx.isSupported(CmfType.ACL)) { throw new ImportException(msg); }
			this.log.warn(msg);
		}
	}

	@Override
	protected final void finalizeConstruction(T object, boolean newObject, DctmImportContext context)
		throws DfException, ImportException {
		super.finalizeConstruction(object, newObject, context);
		doFinalizeConstruction(object, newObject, context);
		// Now, link to the parent folders
		linkToParents(object, context);
		if (!isReference()) {
			restoreAcl(object, context);
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
		context.getAttributeMapper().setMapping(this.cmfObject.getType(), DctmAttributes.R_OBJECT_ID,
			this.cmfObject.getId(), newId.getId());
		return newId;
	}

	protected IDfId persistNewVersion(T sysObject, String versionLabel, DctmImportContext context) throws DfException {
		String vl = (versionLabel != null ? versionLabel
			: DfUtils.concatenateStrings(this.cmfObject.getAttribute(DctmAttributes.R_VERSION_LABEL), ','));
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
		this.log.info(String.format("%s %s [%s](%s) to CMS as version [%s] (newId=%s)", action,
			this.cmfObject.getType(), this.cmfObject.getLabel(), this.cmfObject.getId(), vl, newId.getId()));
		return newId;
	}

	@Override
	protected void finalizeOperation(T sysObject) throws DfException, ImportException {
	}

	@Override
	protected String generateSystemAttributesSQL(CmfObject<IDfValue> stored, IDfPersistentObject sysObject,
		DctmImportContext ctx) throws DfException {
		if (!(sysObject instanceof IDfSysObject)) {
			// how, exactly, did we get here?
			return null;
		}

		final IDfSession session = sysObject.getSession();

		// prepare sql to be executed
		// Important: REPLACE QUOTES!!

		CmfAttribute<IDfValue> modifyDateAtt = stored.getAttribute(DctmAttributes.R_MODIFY_DATE);
		final String modifyDate;
		if (modifyDateAtt != null) {
			modifyDate = DfUtils.generateSqlDateClause(modifyDateAtt.getValue().asTime().getDate(), session);
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
		modifierName = DfUtils.sqlQuoteString(DctmMappingUtils.resolveMappableUser(session, modifierName));

		CmfAttribute<IDfValue> creationDateAtt = stored.getAttribute(DctmAttributes.R_CREATION_DATE);
		final String creationDate;
		if (creationDateAtt != null) {
			creationDate = DfUtils.generateSqlDateClause(creationDateAtt.getValue().asTime().getDate(), session);
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
		creatorName = DfUtils.sqlQuoteString(DctmMappingUtils.resolveMappableUser(session, creatorName));

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
			vstampFlag, DfUtils.sqlQuoteString(sysObject.getObjectId().getId()));
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
		CmfAttribute<IDfValue> att = this.cmfObject.getAttribute(DctmAttributes.I_IS_REFERENCE);
		return ((att != null) && att.hasValues() && att.getValue().asBoolean());
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
		Mapping m = context.getAttributeMapper().getTargetMapping(this.cmfObject.getType(), DctmAttributes.R_OBJECT_ID,
			referenceById.asString());
		if (m != null) {
			referenceById = DfValueFactory.newIdValue(m.getTargetValue());
		}

		target = IDfSysObject.class.cast(session.getObject(referenceById.asId()));
		if (!(target instanceof IDfSysObject)) { throw new ImportException(
			String.format("Reference [%s] target object [%s] is not an IDfSysObject instance",
				this.cmfObject.getLabel(), referenceById.asString())); }

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

		IDfCollection c = null;
		final IDfReference ref;
		try {
			c = DfUtils.executeQuery(session,
				String.format("select r_object_id from dm_reference_s where r_mirror_object_id = %s",
					DfUtils.quoteString(newId.getId())));
			if (!c.next()) {
				// ERROR!
				throw new ImportException(String.format("Reference [%s] could not be found with its new ID [%s]",
					this.cmfObject.getLabel(), newId.getId()));
			}
			ref = IDfReference.class.cast(session.getObject(c.getId("r_object_id")));
		} finally {
			DfUtils.closeQuietly(c);
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
		if ((p == null) || (p.getValueCount() == 0)) { throw new ImportException(String
			.format("No target paths specified for [%s](%s)", this.cmfObject.getLabel(), this.cmfObject.getId())); }
		return p.getValues();
	}

	protected T locateExistingByPath(DctmImportContext ctx) throws ImportException, DfException {
		final IDfSession session = ctx.getSession();
		final String documentName = this.cmfObject.getAttribute(DctmAttributes.OBJECT_NAME).getValue().asString();

		IDfType type = DctmTranslator.translateType(ctx, this.cmfObject);
		if (type == null) { throw new ImportException(String.format(
			"Unsupported subtype [%s] and object type [%s] in object [%s](%s)", this.cmfObject.getSubtype(),
			this.cmfObject.getType(), this.cmfObject.getLabel(), this.cmfObject.getId())); }

		final String dqlBase = String.format("%s (ALL) where object_name = %s and folder(%%s)", type.getName(),
			DfUtils.quoteString(documentName));

		final boolean seeksReference = isReference();
		String existingPath = null;
		T existing = null;
		final Class<T> dfClass = getObjectClass();
		for (IDfValue p : getTargetPaths()) {
			final String targetPath = ctx.getTargetPath(p.asString());
			final String dql = String.format(dqlBase, DfUtils.quoteString(targetPath));
			final String currentPath = String.format("%s/%s", targetPath, documentName);
			IDfPersistentObject current = session.getObjectByQualification(dql);
			if (current == null) {
				// No match, we're good...
				continue;
			}

			// Verify hierarchy
			if (!current.getType().isTypeOf(type.getName())) {
				// Not a document...we have a problem
				throw new ImportException(String.format(
					"Found an incompatible object in one of the %s [%s] %s's intended paths: [%s] = [%s:%s]",
					this.cmfObject.getSubtype(), this.cmfObject.getLabel(), this.cmfObject.getSubtype(), currentPath,
					current.getType().getName(), current.getObjectId().getId()));
			}

			T currentObj = dfClass.cast(current);
			if (isDfReference(currentObj) != seeksReference) {
				// The target document's reference flag is different from ours...problem!
				throw new ImportException(String.format(
					"Reference flag mismatch between objects. The [%s] %s collides with a %sreference at [%s] (%s:%s)",
					this.cmfObject.getLabel(), this.cmfObject.getSubtype(), (seeksReference ? "non-" : ""), currentPath,
					current.getType().getName(), current.getObjectId().getId()));
			}

			if (existing == null) {
				// First match, keep track of it
				existing = currentObj;
				existingPath = currentPath;
				continue;
			}

			// Second match, is it the same as the first?
			if (Tools.equals(existing.getObjectId().getId(), current.getObjectId().getId())) {
				// Same as the first - we have no issue here
				continue;
			}

			// Not the same, this is a problem
			throw new ImportException(
				String.format("Found two different documents matching the [%s] document's paths: [%s@%s] and [%s@%s]",
					this.cmfObject.getLabel(), existing.getObjectId().getId(), existingPath,
					current.getObjectId().getId(), currentPath));
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
				this.log.warn(String.format("Fixup path [%s] for %s [%s](%s) was not found", path,
					this.cmfObject.getType(), this.cmfObject.getLabel(), this.cmfObject.getId()));
			}
		}
		Mapping m = context.getAttributeMapper().getTargetMapping(DctmObjectType.FOLDER.getStoredObjectType(),
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
		final IDfSession session = sysObject.getSession();

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
			DfUtils.lockObject(this.log, parent);
			parent.fetch(null);
			parentCache.put(parentId, parent);
		}

		// Ok...so now we have the parents locked, in the right order, and the object
		// for the parent is cached...proceed with the unlink first
		this.parentLinkActions = new ArrayList<>(lockingOrder.size());
		for (String parentId : oldParents) {
			IDfSysObject parent = parentCache.get(parentId);
			ParentFolderAction action = new ParentFolderAction(parent, false);
			this.log.debug(String.format("Applying %s", action));
			action.apply(sysObject);
			this.log.debug(String.format("Applied %s", action));
			this.parentLinkActions.add(action);
		}

		for (String parentId : newParents) {
			IDfSysObject parent = parentCache.get(parentId);
			ParentFolderAction action = new ParentFolderAction(parent, true);
			this.log.debug(String.format("Applying %s", action));
			action.apply(sysObject);
			this.log.debug(String.format("Applied %s", action));
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
		if (ctx.isSupported(CmfType.DATASTORE)) {
			CmfAttribute<IDfValue> att = this.cmfObject.getAttribute(DctmAttributes.A_STORAGE_TYPE);
			String dataStore = "";
			if ((att != null) && att.hasValues()) {
				dataStore = att.getValue().asString();
			}
			if (!StringUtils.isBlank(dataStore) && !Tools.equals(newObj.getStorageType(), dataStore)) {
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
					if (ctx.isSupported(CmfType.USER)) { throw new ImportException(msg); }
					this.log.warn(msg);
				}
			} catch (MultipleUserMatchesException e) {
				String msg = String.format("Failed to set the owner for %s [%s](%s) to user [%s] - %s",
					this.cmfObject.getType(), this.cmfObject.getLabel(), sysObject.getObjectId().getId(), actualUser,
					e.getMessage());
				if (ctx.isSupported(CmfType.USER)) { throw new ImportException(msg); }
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
				if (ctx.isSupported(CmfType.GROUP)) { throw new ImportException(msg); }
				this.log.warn(msg);
			}
		}
	}

	@Override
	protected ImportOutcome doImportObject(DctmImportContext context) throws DfException, ImportException {
		// First things first: fix the parent paths in the incoming object
		CmfProperty<IDfValue> paths = this.cmfObject.getProperty(IntermediateProperty.PATH);
		if (paths == null) {
			paths = new CmfProperty<>(IntermediateProperty.PATH, CmfDataType.STRING, true);
			this.cmfObject.setProperty(paths);
			this.log.warn(String.format("Added the %s property for [%s](%s) (missing at the source)",
				IntermediateProperty.PATH, this.cmfObject.getLabel(), this.cmfObject.getId()));
		}

		CmfProperty<IDfValue> parents = this.cmfObject.getProperty(IntermediateProperty.PARENT_ID);
		if (parents == null) {
			parents = new CmfProperty<>(IntermediateProperty.PARENT_ID, CmfDataType.ID, true);
			this.cmfObject.setProperty(parents);
			this.log.warn(String.format("Added the %s property for [%s](%s) (missing at the source)",
				PropertyIds.PARENT_ID, this.cmfObject.getLabel(), this.cmfObject.getId()));
		}

		return super.doImportObject(context);
	}
}