/**
 *
 */

package com.delta.cmsmf.cms;

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

import org.apache.commons.lang3.text.StrTokenizer;
import org.apache.log4j.Logger;

import com.armedia.commons.utilities.Tools;
import com.delta.cmsmf.cfg.Constant;
import com.delta.cmsmf.cms.CmsAttributeMapper.Mapping;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.utils.DfUtils;
import com.delta.cmsmf.utils.DfVersionNumber;
import com.delta.cmsmf.utils.DfVersionTree;
import com.documentum.fc.client.DfIdNotFoundException;
import com.documentum.fc.client.DfPermit;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfPermit;
import com.documentum.fc.client.IDfPermitType;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.IDfType;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfTime;
import com.documentum.fc.common.IDfValue;

abstract class CmsSysObject<T extends IDfSysObject> extends CmsObject<T> {

	// Disable, for now, since it messes up with version number copying
	// private static final Pattern INTERNAL_VL = Pattern.compile("^\\d+(\\.\\d+)+$");
	private static final Collection<String> NO_PERMITS = Collections.emptySet();

	protected static final String BRANCH_MARKER = "contents";

	protected static final String TARGET_PATHS = "targetPaths";
	protected static final String TARGET_PARENTS = "targetParents";

	private static final Set<String> AUTO_PERMITS;

	static {
		Set<String> s = new HashSet<String>();
		s.add(IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR);
		s.add(IDfACL.DF_XPERMIT_EXECUTE_PROC_STR);
		AUTO_PERMITS = Collections.unmodifiableSet(s);
	}

	protected static final class TemporaryPermission {
		private final Logger log = Logger.getLogger(getClass());

		private final String objectId;
		private final IDfPermit oldPermit;
		private final IDfPermit newPermit;
		private final Set<String> newXPermit;
		private final Set<String> autoRemove;

		public TemporaryPermission(IDfSysObject object, int newPermission, String... newXPermits) throws DfException {
			this(object, newPermission, (newXPermits == null ? CmsSysObject.NO_PERMITS : Arrays.asList(newXPermits)));
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
			for (String x : CmsSysObject.AUTO_PERMITS) {
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
		private final Logger log = Logger.getLogger(getClass());

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
				this.log.debug(String.format("LINKING [%s] --> [%s]", this.link ? "" : "UN", child.getObjectId()
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

	private boolean mustFreeze = false;
	private boolean mustImmute = false;
	private TemporaryPermission existingTemporaryPermission = null;
	private Collection<ParentFolderAction> parentLinkActions = null;

	/**
	 * @param dfClass
	 */
	public CmsSysObject(Class<T> dfClass) {
		super(dfClass);
	}

	@Override
	protected boolean isTransitoryObject(T sysObject) throws DfException, CMSMFException {
		return sysObject.isCheckedOut();
	}

	protected final void detectIncomingMutability() {
		if (!this.mustFreeze) {
			CmsAttribute frozen = getAttribute(CmsAttributes.R_FROZEN_FLAG);
			if (frozen != null) {
				// We only copy over the "true" values - we don't override local frozen status
				// if it's set to true, and the incoming value is false
				this.mustFreeze |= frozen.getValue().asBoolean();
			}
		}
		if (!this.mustFreeze) {
			CmsAttribute immutable = getAttribute(CmsAttributes.R_IMMUTABLE_FLAG);
			if (immutable != null) {
				// We only copy over the "true" values - we don't override local immutable
				// status if it's set to true, and the incoming value is false
				this.mustImmute |= immutable.getValue().asBoolean();
			}
		}
	}

	protected final void detectAndClearMutability(T sysObject) throws DfException {
		this.mustFreeze = false;
		this.mustImmute = false;
		final String newId = sysObject.getObjectId().getId();
		if (sysObject.isFrozen()) {
			// An object being frozen implies immutability
			this.mustFreeze = true;
			if (this.log.isDebugEnabled()) {
				this.log.debug(String.format("Clearing frozen status from [%s](%s){%s}", getLabel(), getId(), newId));
			}
			// TODO: How to determine if we use false or true here? Stick to false, for now...
			sysObject.unfreeze(false);
			if (!sysObject.isCheckedOut()) {
				sysObject.save();
			}
		}
		// Freezing implies immutability
		if (!this.mustFreeze && sysObject.isImmutable()) {
			// An object may be immutable, yet not frozen
			this.mustImmute = true;
			if (this.log.isDebugEnabled()) {
				this.log
					.debug(String.format("Clearing immutable status from [%s](%s){%s}", getLabel(), getId(), newId));
			}
			sysObject.setBoolean(CmsAttributes.R_IMMUTABLE_FLAG, false);
			if (!sysObject.isCheckedOut()) {
				sysObject.save();
			}
		}
		detectIncomingMutability();
	}

	protected final boolean restoreMutability(T sysObject) throws DfException {
		boolean ret = false;
		final String newId = sysObject.getObjectId().getId();
		if (this.mustFreeze && !sysObject.isFrozen()) {
			if (this.log.isDebugEnabled()) {
				this.log.debug(String.format("Setting frozen status to [%s](%s){%s}", getLabel(), getId(), newId));
			}
			// TODO: assembly support?
			// TODO: How to determine if we use false or true here? Stick to false, for now...
			sysObject.freeze(false);
			ret |= true;
		} else if (this.mustImmute && !sysObject.isImmutable()) {
			if (this.log.isDebugEnabled()) {
				this.log
					.debug(String.format("Setting immutability status to [%s](%s){%s}", getLabel(), getId(), newId));
			}
			sysObject.setBoolean(CmsAttributes.R_IMMUTABLE_FLAG, true);
			ret |= true;
		}
		return ret;
	}

	@Override
	protected void getDataProperties(Collection<CmsProperty> properties, T object, CmsTransferContext ctx)
		throws DfException, CMSMFException {
		IDfSession session = object.getSession();
		CmsProperty paths = new CmsProperty(CmsSysObject.TARGET_PATHS, CmsDataType.DF_STRING, true);
		properties.add(paths);
		CmsProperty parents = new CmsProperty(CmsSysObject.TARGET_PARENTS, CmsDataType.DF_ID, true);
		properties.add(parents);
		for (IDfValue folderId : getAttribute(CmsAttributes.I_FOLDER_ID)) {
			final IDfFolder parent;
			try {
				parent = session.getFolderBySpecification(folderId.asId().getId());
			} catch (DfIdNotFoundException e) {
				this.log.warn(String.format("%s [%s](%s) references non-existent folder [%s]", getType().name(),
					getLabel(), getId(), folderId.asString()));
				continue;
			}
			parents.addValue(folderId);
			int pathCount = parent.getFolderPathCount();
			for (int i = 0; i < pathCount; i++) {
				paths.addValue(DfValueFactory.newStringValue(parent.getFolderPath(i)));
			}
		}
	}

	@Override
	protected void prepareOperation(T sysObject, boolean newObject) throws DfException, CMSMFException {
		if (!isTransitoryObject(sysObject)) {
			this.existingTemporaryPermission = new TemporaryPermission(sysObject, IDfACL.DF_PERMIT_DELETE);
			if (this.existingTemporaryPermission.grant(sysObject)) {
				sysObject.save();
			}
		}
	}

	@Override
	protected boolean cleanupAfterSave(T object, boolean newObject, CmsTransferContext context) throws DfException,
		CMSMFException {
		boolean ret = restoreMutability(object);
		ret |= (this.existingTemporaryPermission != null) && this.existingTemporaryPermission.revoke(object);
		return ret;
	}

	@Override
	protected IDfId persistChanges(T sysObject, CmsTransferContext context) throws DfException, CMSMFException {
		if (sysObject.isImmutable() || sysObject.isFrozen()) {
			CmsAttribute att = getAttribute(CmsAttributes.R_IMMUTABLE_FLAG);
			final boolean sourceImmute = att.getValue().asBoolean();
			att = getAttribute(CmsAttributes.R_FROZEN_FLAG);
			final boolean sourceFreeze = att.getValue().asBoolean();
			this.log.warn(String.format("%s [%s](%s) is unexpectedly %s (expected=[%s|%s] / source=[%s|%s])",
				getType(), getLabel(), getId(), sysObject.isFrozen() ? "FROZEN" : "IMMUTABLE", this.mustFreeze,
				this.mustImmute, sourceFreeze, sourceImmute));
		}

		if (!sysObject.isCheckedOut()) { return super.persistChanges(sysObject, context); }
		final String vl = getAttribute(CmsAttributes.R_VERSION_LABEL).getConcatenatedString(",");
		IDfValue branchMarker = context.getValue(CmsSysObject.BRANCH_MARKER);
		final IDfId newId;
		final String action;
		if ((branchMarker == null) || !branchMarker.asBoolean()) {
			action = "Checked in";
			newId = sysObject.checkin(false, vl);
		} else {
			action = "Branched";
			newId = sysObject.getObjectId();
			sysObject.save();
		}
		this.log.info(String.format("%s %s [%s](%s) to CMS as version [%s] (newId=%s)", action, getType(), getLabel(),
			getId(), vl, newId.getId()));
		context.getAttributeMapper().setMapping(getType(), CmsAttributes.R_OBJECT_ID, getId(), newId.getId());
		return newId;
	}

	@Override
	protected void finalizeOperation(T sysObject) throws DfException, CMSMFException {
	}

	@Override
	protected String generateSystemAttributesSQL(IDfPersistentObject sysObject) throws DfException {
		if (!(sysObject instanceof IDfSysObject)) {
			// how, exactly, did we get here?
			return null;
		}

		final IDfSession session = sysObject.getSession();

		// prepare sql to be executed
		CmsAttribute modifyDateAtt = getAttribute(CmsAttributes.R_MODIFY_DATE);
		CmsAttribute modifierNameAtt = getAttribute(CmsAttributes.R_MODIFIER);
		CmsAttribute creationDateAtt = getAttribute(CmsAttributes.R_CREATION_DATE);
		CmsAttribute creatorNameAtt = getAttribute(CmsAttributes.R_CREATOR_NAME);
		CmsAttribute deletedAtt = getAttribute(CmsAttributes.I_IS_DELETED);
		CmsAttribute aclDomainAtt = getAttribute(CmsAttributes.ACL_DOMAIN);

		// Important: REPLACE QUOTES!!

		// Make sure we ALWAYS store this, even if it's "null"...default to the connected user
		// if not set
		String creatorName = creatorNameAtt.getValue().asString();
		if (creatorName.length() == 0) {
			creatorName = "${owner_name}";
		}
		creatorName = CmsMappingUtils.resolveMappableUser(session, creatorName);
		creatorName = creatorName.replaceAll("'", "''''");

		String modifierName = modifierNameAtt.getValue().asString();
		if (modifierName.length() == 0) {
			modifierName = "${owner_name}";
		}
		modifierName = CmsMappingUtils.resolveMappableUser(session, modifierName);
		modifierName = modifierName.replaceAll("'", "''''");

		String aclDomain = aclDomainAtt.getValue().asString();
		if (aclDomain.length() == 0) {
			aclDomain = "${owner_name}";
		}
		aclDomain = CmsMappingUtils.resolveMappableUser(session, aclDomain);
		aclDomain = aclDomain.replaceAll("'", "''''");

		String aclName = getAttribute(CmsAttributes.ACL_NAME).getValue().asString();

		IDfTime modifyDate = modifyDateAtt.getValue().asTime();
		IDfTime creationDate = creationDateAtt.getValue().asTime();

		String sql = "" //
			+ "UPDATE dm_sysobject_s SET " //
			+ "       r_modify_date = %s, " //
			+ "       r_modifier = ''%s'', " //
			+ "       r_creation_date = %s, " //
			+ "       r_creator_name = ''%s'', " //
			+ "       acl_name = ''%s'', " //
			+ "       acl_domain = ''%s'', " //
			+ "       i_is_deleted = %d " //
			+ "       %s " //
			+ " WHERE r_object_id = ''%s''";
		String vstampFlag = "";
		// TODO: For now we don't touch the i_vstamp b/c we don't think it necessary
		// (Setting.SKIP_VSTAMP.getBoolean() ? "" : String.format(", i_vstamp = %d",
		// dctmObj.getIntSingleAttrValue(CmsAttributes.I_VSTAMP)));
		return String.format(sql, DfUtils.generateSqlDateClause(modifyDate, session), modifierName, DfUtils
			.generateSqlDateClause(creationDate, session), creatorName, aclName, aclDomain, (deletedAtt.getValue()
			.asBoolean() ? 1 : 0), vstampFlag, sysObject.getObjectId().getId());
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

	protected Collection<IDfValue> getTargetPaths() throws DfException, CMSMFException {
		return getProperty(CmsSysObject.TARGET_PATHS).getValues();
	}

	protected T locateExistingByPath(CmsTransferContext ctx) throws CMSMFException, DfException {
		final IDfSession session = ctx.getSession();
		final String documentName = getAttribute(CmsAttributes.OBJECT_NAME).getValue().asString();
		final String quotedDocumentName = documentName.replace("'", "''");

		final String dqlBase = String.format("%s (ALL) where object_name = '%s' and folder('%%s')", getSubtype(),
			quotedDocumentName);

		final boolean seeksReference = isReference();
		String existingPath = null;
		T existing = null;
		final IDfType type = session.getType(getSubtype());
		final Class<T> dfClass = getDfClass();
		for (IDfValue p : getTargetPaths()) {
			final String dql = String.format(dqlBase, p.asString());
			final String currentPath = String.format("%s/%s", p.asString(), documentName);
			IDfPersistentObject current = session.getObjectByQualification(dql);
			if (current == null) {
				// No match, we're good...
				continue;
			}

			// Verify hierarchy
			if (!current.getType().isTypeOf(type.getName())) {
				// Not a document...we have a problem
				throw new CMSMFException(String.format(
					"Found an incompatible object in one of the %s [%s] %s's intended paths: [%s] = [%s:%s]",
					getSubtype(), getLabel(), getSubtype(), currentPath, current.getType().getName(), current
						.getObjectId().getId()));
			}

			T currentObj = dfClass.cast(current);
			if (isDfReference(currentObj) != seeksReference) {
				// The target document's reference flag is different from ours...problem!
				throw new CMSMFException(String.format(
					"Reference flag mismatch between objects. The [%s] %s collides with a %sreference at [%s] (%s:%s)",
					getLabel(), getSubtype(), (seeksReference ? "non-" : ""), currentPath, current.getType().getName(),
					current.getObjectId().getId()));
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
			throw new CMSMFException(String.format(
				"Found two different documents matching the [%s] document's paths: [%s@%s] and [%s@%s]", getLabel(),
				existing.getObjectId().getId(), existingPath, current.getObjectId().getId(), currentPath));
		}

		return existing;
	}

	@Override
	protected T locateInCms(CmsTransferContext ctx) throws CMSMFException, DfException {
		return locateExistingByPath(ctx);
	}

	/**
	 * <p>
	 * Returns the list of all the object ID's for the given {@link IDfSysObject}'s version
	 * chronicle, in the correct historical order for re-creation. This doesn't necessarily mean
	 * that it'll be the exactly correct historical order, chronologically. This is simply a
	 * flattened representation of the version tree such that it is guaranteed that while walking
	 * the list in order, one will never traverse a descendent before traversing its antecedent
	 * version.
	 * </p>
	 * <p>
	 * In particular, branches are not guaranteed to be returned in any order relative to their
	 * siblings; where there direct hierarchical relationships between branches, child branches will
	 * <b>always</b> come after their parent branches. Furthermore, branch nodes may be mixed in
	 * together such that one cannot expect branches to be followed through continually in segments.
	 * <b><i>The only guarantee offered on the items being returned is that for any given item on
	 * the list (except the first item for obvious reasons), its antecedent version will precede it
	 * on the list. How far ahead that antecedent is, is undefined and no expectation should be held
	 * on that. </i></b>
	 * </p>
	 * <p>
	 * The first element on the list is always guaranteed to be the root of the chronicle (i.e.
	 * {@code r_object_id == i_chronicle_id}). The last element is not guaranteed to be the
	 * absolutely latest element in the entire version tree, while it is definitely guaranteed to be
	 * the very latest element in its own version branch ({@code i_latest_flag == true}).
	 * </p>
	 *
	 * @param object
	 * @return the list of all the object ID's for the given {@link IDfSysObject}'s version
	 *         chronicle, in the correct historical order for re-creation
	 * @throws DfException
	 * @throws CMSMFException
	 */
	protected final List<T> getVersionHistory(T object, CmsTransferContext ctx) throws DfException, CMSMFException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object whose versions to analyze"); }
		final IDfSession session = object.getSession();
		final IDfId chronicleId = object.getChronicleId();

		@SuppressWarnings("unchecked")
		List<T> history = (List<T>) ctx.getObject(Constant.VERSION_HISTORY);
		if (history != null) {
			//
			return history;
		}

		// No existing history, we must calculate it
		history = new LinkedList<T>();
		DfVersionTree tree = new DfVersionTree(session, chronicleId);
		List<IDfValue> patches = new ArrayList<IDfValue>();
		Map<String, List<IDfValue>> versionPatches = new HashMap<String, List<IDfValue>>();
		for (DfVersionNumber versionNumber : tree.allVersions) {
			if (tree.totalPatches.contains(versionNumber)) {
				patches.add(DfValueFactory.newStringValue(versionNumber.toString()));
				continue;
			}

			final IDfId id = new DfId(tree.indexByVersionNumber.get(versionNumber));
			final T entry = castObject(session.getObject(id));
			history.add(entry);
			if (!patches.isEmpty()) {
				patches = Tools.freezeList(patches);
				versionPatches.put(id.getId(), patches);
				patches = new ArrayList<IDfValue>();
			}
		}
		// Only put this in the context when it's needed
		versionPatches = Tools.freezeMap(versionPatches);
		ctx.setObject(Constant.VERSION_PATCHES, versionPatches);
		history = Tools.freezeList(history);
		ctx.setObject(Constant.VERSION_HISTORY, history);
		return history;
	}

	protected T newVersionTreePatch(T base, DfVersionNumber patchNumber) {
		throw new AbstractMethodError("Must implement newPatch() to support version tree repair");
	}

	protected List<String> getProspectiveParents(CmsTransferContext context) throws DfException {
		CmsProperty parents = getProperty(CmsSysObject.TARGET_PARENTS);
		List<String> newParents = new ArrayList<String>(parents.getValueCount());
		CmsAttributeMapper mapper = context.getAttributeMapper();
		for (int i = 0; i < parents.getValueCount(); i++) {
			IDfId parentId = parents.getValue(i).asId();
			// We already know the parents are folders, b/c that's how we harvested them in the
			// export, so we stick to that
			Mapping m = mapper.getTargetMapping(CmsObjectType.FOLDER, CmsAttributes.R_OBJECT_ID, parentId.getId());
			if (m == null) {
				// TODO: HOW??! Must have been an import failure on the parent...
				continue;
			}

			newParents.add(m.getTargetValue());
		}
		return newParents;
	}

	protected final boolean linkToParents(T sysObject, CmsTransferContext ctx) throws DfException, CMSMFException {
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

	protected final void cleanUpParents(IDfSession session) throws DfException, CMSMFException {
		if (this.parentLinkActions != null) {
			for (ParentFolderAction action : this.parentLinkActions) {
				action.cleanUp();
			}
		}
	}
}