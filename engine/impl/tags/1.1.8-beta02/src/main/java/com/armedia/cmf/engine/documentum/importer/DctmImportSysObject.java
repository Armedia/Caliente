/**
 *
 */

package com.armedia.cmf.engine.documentum.importer;

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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.engine.documentum.DctmAttributes;
import com.armedia.cmf.engine.documentum.DctmMappingUtils;
import com.armedia.cmf.engine.documentum.DctmObjectType;
import com.armedia.cmf.engine.documentum.DctmTranslator;
import com.armedia.cmf.engine.documentum.DfUtils;
import com.armedia.cmf.engine.documentum.common.DctmSysObject;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.storage.StoredAttribute;
import com.armedia.cmf.storage.StoredAttributeMapper;
import com.armedia.cmf.storage.StoredAttributeMapper.Mapping;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredProperty;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.DfPermit;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfGroup;
import com.documentum.fc.client.IDfPermit;
import com.documentum.fc.client.IDfPermitType;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.IDfType;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfValue;

public abstract class DctmImportSysObject<T extends IDfSysObject> extends DctmImportDelegate<T> implements
DctmSysObject {

	// Disable, for now, since it messes up with version number copying
	// private static final Pattern INTERNAL_VL = Pattern.compile("^\\d+(\\.\\d+)+$");
	private static final Collection<String> NO_PERMITS = Collections.emptySet();

	protected static final String BRANCH_MARKER = "branchMarker";

	private static final Set<String> AUTO_PERMITS;

	static {
		Set<String> s = new HashSet<String>();
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
			this(object, newPermission, (newXPermits == null ? DctmImportSysObject.NO_PERMITS : Arrays
				.asList(newXPermits)));
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

			Set<String> s = new HashSet<String>(StrTokenizer.getCSVInstance(object.getXPermitList()).getTokenList());
			Set<String> autoRemove = new HashSet<String>();
			for (String x : DctmImportSysObject.AUTO_PERMITS) {
				if (!s.contains(x)) {
					autoRemove.add(x);
				}
			}
			this.autoRemove = Collections.unmodifiableSet(autoRemove);

			// Now the ones we're adding
			Set<String> nx = new TreeSet<String>();
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
			if (!Tools.equals(this.objectId, object.getObjectId().getId())) { throw new DfException(
				String.format("ERROR: Expected object with ID [%s] but got [%s] instead", this.objectId, object
					.getObjectId().getId())); }
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
					this.log.debug(String.format("%s [%s] on [%s]", (grant ? "GRANTING" : "REVOKING"), p,
						object.getObjectId()));
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
				boolean ok = false;
				try {
					if (this.log.isDebugEnabled()) {
						this.log.debug(String.format("LOCKING [%s]", this.parentId));
					}
					this.parent.lock();
					if (this.log.isDebugEnabled()) {
						this.log.debug(String.format("LOCKED [%s]", this.parentId));
					}
					ok = true;
				} finally {
					if (!ok) {
						hashCode();
					}
				}
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
				this.log.debug(String.format("%sLINKING [%s] --> [%s]", this.link ? "" : "UN", child.getObjectId()
					.getId(), this.parent.getObjectId().getId()));
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

	public DctmImportSysObject(DctmImportEngine engine, DctmObjectType expectedType, StoredObject<IDfValue> storedObject) {
		super(engine, expectedType, storedObject);
	}

	@Override
	protected boolean isTransitoryObject(T sysObject) throws DfException, ImportException {
		return sysObject.isCheckedOut();
	}

	protected final void detectIncomingMutability() {
		if (!this.mustFreeze) {
			StoredAttribute<IDfValue> frozen = this.storedObject.getAttribute(DctmAttributes.R_FROZEN_FLAG);
			if (frozen != null) {
				// We only copy over the "true" values - we don't override local frozen status
				// if it's set to true, and the incoming value is false
				this.mustFreeze |= frozen.getValue().asBoolean();
			}
		}
		if (!this.mustFreeze) {
			StoredAttribute<IDfValue> immutable = this.storedObject.getAttribute(DctmAttributes.R_IMMUTABLE_FLAG);
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
				this.log.debug(String.format("Clearing frozen status from [%s](%s){%s}", this.storedObject.getLabel(),
					this.storedObject.getId(), newId));
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
				this.log.debug(String.format("Clearing immutable status from [%s](%s){%s}",
					this.storedObject.getLabel(), this.storedObject.getId(), newId));
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
				this.log.debug(String.format("Setting frozen status to [%s](%s){%s}", this.storedObject.getLabel(),
					this.storedObject.getId(), newId));
			}
			// TODO: assembly support?
			// TODO: How to determine if we use false or true here? Stick to false, for now...
			sysObject.freeze(false);
			ret |= true;
		} else if (this.mustImmute && !sysObject.isImmutable()) {
			if (this.log.isDebugEnabled()) {
				this.log.debug(String.format("Setting immutability status to [%s](%s){%s}",
					this.storedObject.getLabel(), this.storedObject.getId(), newId));
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
	protected void prepareOperation(T sysObject, boolean newObject) throws DfException, ImportException {
		if (!isTransitoryObject(sysObject)) {
			this.existingTemporaryPermission = new TemporaryPermission(sysObject, IDfACL.DF_PERMIT_DELETE);
			if (this.existingTemporaryPermission.grant(sysObject)) {
				sysObject.save();
			}
		}
	}

	@Override
	protected boolean cleanupAfterSave(T object, boolean newObject, DctmImportContext context) throws DfException,
	ImportException {
		boolean ret = restoreMutability(object);
		ret |= (this.existingTemporaryPermission != null) && this.existingTemporaryPermission.revoke(object);
		return ret;
	}

	@Override
	protected IDfId persistChanges(T sysObject, DctmImportContext context) throws DfException, ImportException {
		if (!sysObject.isCheckedOut()) { return super.persistChanges(sysObject, context); }
		IDfId newId = persistNewVersion(sysObject, null, context);
		context.getAttributeMapper().setMapping(this.storedObject.getType(), DctmAttributes.R_OBJECT_ID,
			this.storedObject.getId(), newId.getId());
		return newId;
	}

	protected IDfId persistNewVersion(T sysObject, String versionLabel, DctmImportContext context) throws DfException {
		String vl = (versionLabel != null ? versionLabel : DfUtils.concatenateStrings(
			this.storedObject.getAttribute(DctmAttributes.R_VERSION_LABEL), ','));
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
			this.storedObject.getType(), this.storedObject.getLabel(), this.storedObject.getId(), vl, newId.getId()));
		return newId;
	}

	@Override
	protected void finalizeOperation(T sysObject) throws DfException, ImportException {
	}

	@Override
	protected String generateSystemAttributesSQL(StoredObject<IDfValue> stored, IDfPersistentObject sysObject,
		DctmImportContext ctx) throws DfException {
		if (!(sysObject instanceof IDfSysObject)) {
			// how, exactly, did we get here?
			return null;
		}

		final IDfSession session = sysObject.getSession();

		// prepare sql to be executed
		// Important: REPLACE QUOTES!!

		StoredAttribute<IDfValue> modifyDateAtt = stored.getAttribute(DctmAttributes.R_MODIFY_DATE);
		final String modifyDate;
		if (modifyDateAtt != null) {
			modifyDate = DfUtils.generateSqlDateClause(modifyDateAtt.getValue().asTime().getDate(), session);
		} else {
			modifyDate = "r_modify_date";
		}

		StoredAttribute<IDfValue> modifierNameAtt = stored.getAttribute(DctmAttributes.R_MODIFIER);
		String modifierName = "";
		if (modifierNameAtt != null) {
			modifierName = modifierNameAtt.getValue().asString();
			try {
				IDfUser u = DctmImportUser.locateExistingUser(session, modifierName, null);
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

		StoredAttribute<IDfValue> creationDateAtt = stored.getAttribute(DctmAttributes.R_CREATION_DATE);
		final String creationDate;
		if (creationDateAtt != null) {
			creationDate = DfUtils.generateSqlDateClause(creationDateAtt.getValue().asTime().getDate(), session);
		} else {
			creationDate = "r_creation_date";
		}

		StoredAttribute<IDfValue> creatorNameAtt = stored.getAttribute(DctmAttributes.R_CREATOR_NAME);
		String creatorName = "";
		if (creatorNameAtt != null) {
			try {
				IDfUser u = DctmImportUser.locateExistingUser(session, creatorNameAtt.getValue().asString(), null);
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

		StoredAttribute<IDfValue> aclNameAtt = stored.getAttribute(DctmAttributes.ACL_NAME);
		StoredAttribute<IDfValue> aclDomainAtt = stored.getAttribute(DctmAttributes.ACL_DOMAIN);
		String aclName = "acl_name";
		String aclDomain = "acl_domain";
		if (ctx.isSupported(StoredObjectType.ACL) && (aclNameAtt != null) && (aclDomainAtt != null)) {
			aclName = DfUtils.sqlQuoteString(aclNameAtt.getValue().asString());
			aclDomain = "";
			if (aclDomainAtt != null) {
				aclDomain = aclDomainAtt.getValue().asString();
			}
			if (aclDomain.length() == 0) {
				aclDomain = "${owner_name}";
			}
			aclDomain = DfUtils.sqlQuoteString(DctmMappingUtils.resolveMappableUser(session, aclDomain));
		}

		StoredAttribute<IDfValue> deletedAtt = stored.getAttribute(DctmAttributes.I_IS_DELETED);
		final boolean deletedFlag = (deletedAtt != null) && deletedAtt.getValue().asBoolean();

		String sql = "" //
			+ "UPDATE dm_sysobject_s SET " //
			+ "       r_modify_date = %s, " //
			+ "       r_modifier = %s, " //
			+ "       r_creation_date = %s, " //
			+ "       r_creator_name = %s, " //
			+ "       acl_name = %s, " //
			+ "       acl_domain = %s, " //
			+ "       i_is_deleted = %d " //
			+ "       %s " //
			+ " WHERE r_object_id = %s";
		String vstampFlag = "";
		// TODO: For now we don't touch the i_vstamp b/c we don't think it necessary
		// (Setting.SKIP_VSTAMP.getBoolean() ? "" : String.format(", i_vstamp = %d",
		// dctmObj.getIntSingleAttrValue(CmsAttributes.I_VSTAMP)));
		return String.format(sql, modifyDate, modifierName, creationDate, creatorName, aclName, aclDomain,
			(deletedFlag ? 1 : 0), vstampFlag, DfUtils.sqlQuoteString(sysObject.getObjectId().getId()));
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
		Set<String> ret = new HashSet<String>();
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
		// TODO: No reference support...yet...uncomment this when testing
		// the reference support
		// return getAttribute(CmsAttributes.I_IS_REFERENCE).getValue().asBoolean();
		return false;
	}

	protected boolean isDfReference(T object) throws DfException {
		// TODO: No reference support...yet...uncomment this when testing
		// the reference support
		// return object.isReference();
		return false;
	}

	@Override
	protected boolean isShortConstructionCycle() {
		// References require a modified algorithm...
		return isReference();
	}

	protected Collection<IDfValue> getTargetPaths() throws DfException, ImportException {
		StoredProperty<IDfValue> p = this.storedObject.getProperty(DctmSysObject.TARGET_PATHS);
		if ((p == null) || (p.getValueCount() == 0)) { throw new ImportException(String.format(
			"No target paths specified for [%s](%s)", this.storedObject.getLabel(), this.storedObject.getId())); }
		return p.getValues();
	}

	protected T locateExistingByPath(DctmImportContext ctx) throws ImportException, DfException {
		final IDfSession session = ctx.getSession();
		final String documentName = this.storedObject.getAttribute(DctmAttributes.OBJECT_NAME).getValue().asString();

		final IDfType type = DctmTranslator.translateType(session, this.storedObject);
		if (type == null) { throw new ImportException(String.format(
			"Unsupported subtype [%s] and object type [%s] in object [%s](%s)", this.storedObject.getSubtype(),
			this.storedObject.getType(), this.storedObject.getLabel(), this.storedObject.getId())); }
		final String dqlBase = String.format("%s (ALL) where object_name = %s and folder(%%s)", type.getName(),
			DfUtils.quoteString(documentName));

		final boolean seeksReference = isReference();
		String existingPath = null;
		T existing = null;
		final Class<T> dfClass = getDfClass();
		for (IDfValue p : getTargetPaths()) {
			final String dql = String.format(dqlBase, DfUtils.quoteString(p.asString()));
			final String currentPath = String.format("%s/%s", p.asString(), documentName);
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
					this.storedObject.getSubtype(), this.storedObject.getLabel(), this.storedObject.getSubtype(),
					currentPath, current.getType().getName(), current.getObjectId().getId()));
			}

			T currentObj = dfClass.cast(current);
			if (isDfReference(currentObj) != seeksReference) {
				// The target document's reference flag is different from ours...problem!
				throw new ImportException(String.format(
					"Reference flag mismatch between objects. The [%s] %s collides with a %sreference at [%s] (%s:%s)",
					this.storedObject.getLabel(), this.storedObject.getSubtype(), (seeksReference ? "non-" : ""),
					currentPath, current.getType().getName(), current.getObjectId().getId()));
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
			throw new ImportException(String.format(
				"Found two different documents matching the [%s] document's paths: [%s@%s] and [%s@%s]",
				this.storedObject.getLabel(), existing.getObjectId().getId(), existingPath, current.getObjectId()
				.getId(), currentPath));
		}

		return existing;
	}

	@Override
	protected T locateInCms(DctmImportContext ctx) throws ImportException, DfException {
		return locateExistingByPath(ctx);
	}

	protected List<String> getProspectiveParents(DctmImportContext context) throws DfException, ImportException {
		StoredProperty<IDfValue> parents = this.storedObject.getProperty(DctmSysObject.TARGET_PARENTS);
		if ((parents == null) || (parents.getValueCount() == 0)) { throw new ImportException(String.format(
			"No target parents specified for [%s](%s)", this.storedObject.getLabel(), this.storedObject.getId())); }
		List<String> newParents = new ArrayList<String>(parents.getValueCount());
		StoredAttributeMapper mapper = context.getAttributeMapper();
		for (int i = 0; i < parents.getValueCount(); i++) {
			String parentId = parents.getValue(i).asString();
			// We already know the parents are folders, b/c that's how we harvested them in the
			// export, so we stick to that
			Mapping m = mapper.getTargetMapping(DctmObjectType.FOLDER.getStoredObjectType(),
				DctmAttributes.R_OBJECT_ID, parentId);
			if (m == null) {
				// TODO: HOW??! Must have been an import failure on the parent...
				continue;
			}

			newParents.add(m.getTargetValue());
		}
		return newParents;
	}

	protected final boolean linkToParents(T sysObject, DctmImportContext ctx) throws DfException, ImportException {
		final IDfSession session = sysObject.getSession();

		// First, get the list of the current parents, and reverse its order.
		// The reverse order will accelerate the unlinking process later on
		final int cpcount = sysObject.getFolderIdCount();
		LinkedList<String> oldParents = new LinkedList<String>();
		for (int i = 0; i < cpcount; i++) {
			oldParents.addLast(sysObject.getFolderId(i).getId());
		}

		List<String> newParents = getProspectiveParents(ctx);
		if (newParents.isEmpty()) {
			getProspectiveParents(ctx);
		}

		// This ensures that we acquire ALL locks in the correct lowest-id-first order,
		// since parentLinkActions will be a sorted tree in that order.
		Set<String> lockingOrder = new TreeSet<String>();
		lockingOrder.addAll(oldParents);
		lockingOrder.addAll(newParents);

		Set<String> common = new HashSet<String>(oldParents);
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
		Map<String, IDfSysObject> parentCache = new HashMap<String, IDfSysObject>(lockingOrder.size());
		for (String parentId : lockingOrder) {
			// We HAVE to lock everything in the correct order prior to modification
			final IDfId id = new DfId(parentId);
			session.flushObject(id);
			final IDfSysObject parent = IDfSysObject.class.cast(session.getObject(id));
			parent.lock();
			parent.fetch(null);
			parentCache.put(parentId, parent);
		}

		// Ok...so now we have the parents locked, in the right order, and the object
		// for the parent is cached...proceed with the unlink first
		this.parentLinkActions = new ArrayList<ParentFolderAction>(lockingOrder.size());
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

		sysObject.save();

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
		return newObj;
	}

	protected void setOwnerGroupACLData(T sysObject, DctmImportContext ctx) throws ImportException, DfException {
		// Set the owner and group
		final IDfSession session = ctx.getSession();
		StoredAttribute<IDfValue> att = this.storedObject.getAttribute(DctmAttributes.OWNER_NAME);
		if (att != null) {
			final String actualUser = DctmMappingUtils.resolveMappableUser(session, att.getValue().asString());
			try {
				IDfUser u = DctmImportUser.locateExistingUser(session, actualUser, null);
				if (u != null) {
					sysObject.setOwnerName(u.getUserName());
				} else {
					String msg = String
						.format(
							"Failed to set the owner for %s [%s](%s) to user [%s] - the user wasn't found - probably didn't need to be copied over",
							this.storedObject.getType(), this.storedObject.getLabel(), sysObject.getObjectId().getId(),
							actualUser);
					if (ctx.isSupported(StoredObjectType.USER)) { throw new ImportException(msg); }
					this.log.warn(msg);
				}
			} catch (MultipleUserMatchesException e) {
				String msg = String.format("Failed to set the owner for %s [%s](%s) to user [%s] - %s",
					this.storedObject.getType(), this.storedObject.getLabel(), sysObject.getObjectId().getId(),
					actualUser, e.getMessage());
				if (ctx.isSupported(StoredObjectType.USER)) { throw new ImportException(msg); }
				this.log.warn(msg);
			}
		}

		att = this.storedObject.getAttribute(DctmAttributes.GROUP_NAME);
		if (att != null) {
			String group = att.getValue().asString();
			IDfGroup g = session.getGroup(group);
			if (g != null) {
				sysObject.setGroupName(g.getGroupName());
			} else {
				String msg = String
					.format(
						"Failed to set the group for %s [%s](%s) to group [%s] - the group wasn't found - probably didn't need to be copied over",
						this.storedObject.getType(), this.storedObject.getLabel(), sysObject.getObjectId().getId(),
						group);
				if (ctx.isSupported(StoredObjectType.GROUP)) { throw new ImportException(msg); }
				this.log.warn(msg);
			}
		}

		// Set the ACL
		StoredAttribute<IDfValue> aclDomainAtt = this.storedObject.getAttribute(DctmAttributes.ACL_DOMAIN);
		StoredAttribute<IDfValue> aclNameAtt = this.storedObject.getAttribute(DctmAttributes.ACL_NAME);
		@SuppressWarnings("unchecked")
		int firstNull = Tools.firstNull(aclDomainAtt, aclNameAtt);
		if (firstNull == -1) {
			String aclDomain = DctmMappingUtils.resolveMappableUser(session, aclDomainAtt.getValue().asString());
			try {
				IDfUser u = DctmImportUser.locateExistingUser(session, aclDomain, null);
				if (u != null) {
					aclDomain = u.getUserName();
					IDfACL acl = session.getACL(aclDomain, aclNameAtt.getValue().asString());
					if (acl != null) {
						sysObject.setACL(acl);
					} else {
						String msg = String
							.format(
								"Failed to set the ACL [%s:%s] for %s [%s](%s) - the ACL wasn't found - probably didn't need to be copied over",
								aclDomain, aclNameAtt.getValue().asString(), this.storedObject.getType(),
								this.storedObject.getLabel(), sysObject.getObjectId().getId());
						if (ctx.isSupported(StoredObjectType.ACL)) { throw new ImportException(msg); }
						this.log.warn(msg);
					}
				} else {
					String msg = String
						.format(
							"Failed to find the user [%s] who owns the ACL for %s [%s](%s) - the user wasn't found - probably didn't need to be copied over",
							aclDomain, this.storedObject.getType(), this.storedObject.getLabel(), sysObject
								.getObjectId().getId());
					if (ctx.isSupported(StoredObjectType.USER)) { throw new ImportException(msg); }
					this.log.warn(msg);
				}
			} catch (MultipleUserMatchesException e) {
				String msg = String.format("Failed to find the user [%s] who owns the ACL for folder [%s](%s) - %s",
					aclDomain, this.storedObject.getLabel(), sysObject.getObjectId().getId(), e.getMessage());
				if (ctx.isSupported(StoredObjectType.USER)) { throw new ImportException(msg); }
				this.log.warn(msg);
			}
		}
	}
}