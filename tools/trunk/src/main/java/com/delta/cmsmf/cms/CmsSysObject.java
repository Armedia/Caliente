/**
 *
 */

package com.delta.cmsmf.cms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.text.StrTokenizer;

import com.armedia.commons.utilities.Tools;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.utils.DfUtils;
import com.documentum.fc.client.DfPermit;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfPermit;
import com.documentum.fc.client.IDfPermitType;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.IDfType;
import com.documentum.fc.client.IDfTypedObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfTime;
import com.documentum.fc.common.IDfValue;

abstract class CmsSysObject<T extends IDfSysObject> extends CmsObject<T> {

	private static final Pattern INTERNAL_VL = Pattern.compile("^\\d+(\\.\\d+)+$");
	private static final Collection<String> NO_PERMITS = Collections.emptySet();

	protected final class PermitDelta {
		private final String objectId;
		private final IDfPermit oldPermit;
		private final IDfPermit newPermit;
		private final Collection<IDfPermit> newXPermit;

		public PermitDelta(IDfSysObject object, int newPermission, String... newXPermits) throws DfException {
			this(object, newPermission, (newXPermits == null ? CmsSysObject.NO_PERMITS : Arrays.asList(newXPermits)));
		}

		public PermitDelta(IDfSysObject object, int newPermission, Collection<String> newXPermits) throws DfException {
			this.objectId = object.getObjectId().getId();
			final String userName = object.getSession().getLoginUserName();

			// Does it have the required access permission?
			int oldPermission = object.getPermit();
			if (oldPermission < newPermission) {
				this.oldPermit = new DfPermit();
				this.oldPermit.setAccessorName(userName);
				this.oldPermit.setPermitType(IDfPermitType.ACCESS_PERMIT);
				this.oldPermit.setPermitValue(DfUtils.decodeAccessPermission(oldPermission));
				this.newPermit = new DfPermit();
				this.newPermit.setAccessorName(userName);
				this.newPermit.setPermitType(IDfPermitType.ACCESS_PERMIT);
				this.newPermit.setPermitValue(DfUtils.decodeAccessPermission(newPermission));
			} else {
				this.oldPermit = null;
				this.newPermit = null;
			}

			Set<String> s = new HashSet<String>(StrTokenizer.getCSVInstance(object.getXPermitList()).getTokenList());
			List<IDfPermit> l = new ArrayList<IDfPermit>();
			for (String x : newXPermits) {
				if (x == null) {
					continue;
				}
				if (!s.contains(x)) {
					IDfPermit xpermit = new DfPermit();
					xpermit.setAccessorName(userName);
					xpermit.setPermitType(IDfPermitType.EXTENDED_PERMIT);
					xpermit.setPermitValue(x);
					l.add(xpermit);
				}
			}
			this.newXPermit = Collections.unmodifiableCollection(l);
		}

		public String getObjectId() {
			return this.objectId;
		}

		private boolean apply(IDfSysObject object, boolean grant) throws DfException {
			if (!Tools.equals(this.objectId, object.getObjectId().getId())) { throw new DfException(
				String.format("ERROR: Expected object with ID [%s] but got [%s] instead", this.objectId, object
					.getObjectId().getId())); }
			boolean ret = false;
			IDfPermit access = (grant ? this.newPermit : this.oldPermit);
			if (access != null) {
				object.grantPermit(access);
				ret = true;
			}
			for (IDfPermit p : this.newXPermit) {
				if (grant) {
					object.grantPermit(p);
				} else {
					object.revokePermit(p);
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
	}

	private boolean mustFreeze = false;
	private boolean mustImmute = false;

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

	@Override
	protected void prepareOperation(T sysObject) throws DfException, CMSMFException {
		this.mustFreeze = false;
		this.mustImmute = false;
		if (sysObject.isFrozen()) {
			this.mustFreeze = true;
			if (this.log.isDebugEnabled()) {
				this.log.debug(String.format("Clearing frozen status from [%s](%s)", getLabel(), getId()));
			}
			sysObject.setBoolean(CmsAttributes.R_FROZEN_FLAG, false);
			if (!sysObject.isCheckedOut()) {
				sysObject.save();
			}
		}
		if (sysObject.isImmutable()) {
			this.mustImmute = true;
			if (this.log.isDebugEnabled()) {
				this.log.debug(String.format("Clearing immutable status from [%s](%s)", getLabel(), getId()));
			}
			sysObject.setBoolean(CmsAttributes.R_IMMUTABLE_FLAG, false);
			if (!sysObject.isCheckedOut()) {
				sysObject.save();
			}
		}

		CmsAttribute frozen = getAttribute(CmsAttributes.R_FROZEN_FLAG);
		if (frozen != null) {
			// We only copy over the "true" values - we don't override local frozen status
			// if it's set to true, and the incoming value is false
			this.mustFreeze |= frozen.getValue().asBoolean();
		}
		CmsAttribute immutable = getAttribute(CmsAttributes.R_IMMUTABLE_FLAG);
		if (immutable != null) {
			// We only copy over the "true" values - we don't override local immutable
			// status if it's set to true, and the incoming value is false
			this.mustImmute |= immutable.getValue().asBoolean();
		}
	}

	@Override
	protected IDfId persistChanges(T sysObject, CmsTransferContext context) throws DfException, CMSMFException {
		if (!sysObject.isCheckedOut()) { return super.persistChanges(sysObject, context); }
		StringBuilder versionLabels = new StringBuilder();
		for (IDfValue v : getAttribute(CmsAttributes.R_VERSION_LABEL)) {
			if (versionLabels.length() > 0) {
				versionLabels.append(',');
			}
			final String label = v.asString();
			// Ignore "internal" version labels
			if (CmsSysObject.INTERNAL_VL.matcher(label).matches()) {
				continue;
			}
			versionLabels.append(label);
		}
		String vl = versionLabels.toString();
		final IDfId newId = sysObject.checkin(false, vl);
		this.log.info(String.format("Checked in %s [%s](%s) to CMS as versions [%s] (newId=%s)", getType(), getLabel(),
			getId(), vl, newId.getId()));
		context.getAttributeMapper().setMapping(getType(), CmsAttributes.R_OBJECT_ID, getId(), newId.getId());
		return newId;
	}

	@Override
	protected void finalizeOperation(T sysObject) throws DfException, CMSMFException {
		if (this.mustImmute) {
			if (this.log.isDebugEnabled()) {
				this.log.debug(String.format("Setting immutability status to [%s](%s)", getLabel(), getId()));
			}
			sysObject.setBoolean(CmsAttributes.R_IMMUTABLE_FLAG, true);
			sysObject.save();
		}
		if (this.mustFreeze) {
			if (this.log.isDebugEnabled()) {
				this.log.debug(String.format("Setting frozen status to [%s](%s)", getLabel(), getId()));
			}
			sysObject.setBoolean(CmsAttributes.R_FROZEN_FLAG, true);
			sysObject.save();
		}
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

	protected abstract Collection<IDfValue> getTargetPaths() throws DfException, CMSMFException;

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
	protected final List<IDfId> getVersionHistory(T object) throws DfException, CMSMFException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object whose versions to analyze"); }

		IDfCollection versions = object
			.getVersions("r_object_id, r_modify_date, r_version_label, i_chronicle_id, i_antecedent_id, i_latest_flag, i_direct_dsc");
		Set<String> finished = new HashSet<String>();
		finished.add(DfId.DF_NULLID_STR); // This helps, below
		LinkedList<IDfId> history = new LinkedList<IDfId>();
		LinkedList<IDfTypedObject> deferred = new LinkedList<IDfTypedObject>();
		try {
			while (versions.next()) {
				IDfId objectId = versions.getId(CmsAttributes.R_OBJECT_ID);
				if (objectId.isNull()) {
					// Shouldn't happen, but better safe than sorry...
					continue;
				}
				IDfId antecedentId = versions.getId(CmsAttributes.I_ANTECEDENT_ID);
				if (finished.contains(antecedentId.getId())) {
					// Antecedent is already in place, add this version
					history.add(objectId);
					finished.add(objectId.getId());
				} else {
					// Antecedent not in yet, defer it...add it at the front
					// because this will help optimize the deferred processing,
					// below, because MOST of the versions will be in the
					// correct order - only a few won't...
					deferred.addFirst(versions.getTypedObject());
				}
			}
		} finally {
			DfUtils.closeQuietly(versions);
		}

		while (!deferred.isEmpty()) {
			Iterator<IDfTypedObject> it = deferred.iterator();
			boolean modified = false;
			while (it.hasNext()) {
				IDfTypedObject v = it.next();
				IDfId objectId = v.getId(CmsAttributes.R_OBJECT_ID);
				IDfId antecedentId = v.getId(CmsAttributes.I_ANTECEDENT_ID);
				if (finished.contains(antecedentId.getId())) {
					// The antecedent is on the list...add this one
					history.add(objectId);
					finished.add(objectId.getId());
					it.remove();
					modified = true;
				}
			}
			if (!modified) {
				// We can't have done two passes without resolving at least one object because
				// that means we have a broken version tree...which is unsupported
				throw new CMSMFException(String.format(
					"Broken version tree found for chronicle [%s] - nodes remaining: %s", object.getChronicleId()
					.getId(), deferred));
			}
		}
		return history;
	}
}