/**
 *
 */

package com.armedia.cmf.documentum.engine.exporter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.armedia.cmf.documentum.engine.DctmAttributes;
import com.armedia.cmf.documentum.engine.DctmObjectType;
import com.armedia.cmf.documentum.engine.DfUtils;
import com.armedia.cmf.engine.exporter.ExportContext;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.storage.StoredObject;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.IDfTypedObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class DctmExportSysObject<T extends IDfSysObject> extends DctmExportAbstract<T> {

	protected DctmExportSysObject(DctmExportEngine engine, DctmObjectType type) {
		super(engine, type);
	}

	protected final String calculateVersionString(IDfSysObject sysObject, boolean full) throws DfException {
		if (!full) { return String.format("%s%s", sysObject.getImplicitVersionLabel(),
			sysObject.getHasFolder() ? ",CURRENT" : ""); }
		int labelCount = sysObject.getVersionLabelCount();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < labelCount; i++) {
			if (i > 0) {
				sb.append(',');
			}
			sb.append(sysObject.getVersionLabel(i));
		}
		return sb.toString();
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
	 * @throws ExportException
	 */
	protected final List<IDfId> getVersionHistory(T object) throws DfException, ExportException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object whose versions to analyze"); }

		IDfCollection versions = object
			.getVersions("r_object_id, r_modify_date, r_version_label, i_chronicle_id, i_antecedent_id, i_latest_flag, i_direct_dsc");
		Set<String> finished = new HashSet<String>();
		finished.add(DfId.DF_NULLID_STR); // This helps, below
		LinkedList<IDfId> history = new LinkedList<IDfId>();
		LinkedList<IDfTypedObject> deferred = new LinkedList<IDfTypedObject>();
		try {
			while (versions.next()) {
				IDfId objectId = versions.getId(DctmAttributes.R_OBJECT_ID);
				if (objectId.isNull()) {
					// Shouldn't happen, but better safe than sorry...
					continue;
				}
				IDfId antecedentId = versions.getId(DctmAttributes.I_ANTECEDENT_ID);
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
				IDfId objectId = v.getId(DctmAttributes.R_OBJECT_ID);
				IDfId antecedentId = v.getId(DctmAttributes.I_ANTECEDENT_ID);
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
				throw new ExportException(String.format(
					"Broken version tree found for chronicle [%s] - nodes remaining: %s", object.getChronicleId()
					.getId(), deferred));
			}
		}
		return history;
	}

	@Override
	protected String calculateLabel(IDfSession session, IDfSysObject sysObject) throws DfException {
		final int folderCount = sysObject.getFolderIdCount();
		final String objectName = sysObject.getObjectName();
		for (int i = 0; i < folderCount; i++) {
			IDfId id = sysObject.getFolderId(i);
			IDfFolder f = IDfFolder.class.cast(session.getFolderBySpecification(id.getId()));
			if (f != null) {
				String path = (f.getFolderPathCount() > 0 ? f.getFolderPath(0) : String.format("(unknown-folder:[%s])",
					id.getId()));
				return String.format("%s/%s [%s]", path, objectName, calculateVersionString(sysObject, true));
			}
		}
		throw new DfException(String.format("None of the parent paths for object [%s] were found", sysObject
			.getObjectId().getId()));
	}

	protected boolean isDfReference(T object) throws DfException {
		// TODO: No reference support...yet...uncomment this when testing
		// the reference support
		// return object.isReference();
		return false;
	}

	@Override
	protected Collection<IDfPersistentObject> findRequirements(IDfSession session, StoredObject<IDfValue> marshaled,
		T sysObject, ExportContext<IDfSession, IDfPersistentObject, IDfValue> ctx) throws Exception {
		Collection<IDfPersistentObject> req = super.findRequirements(session, marshaled, sysObject, ctx);
		// The parent folders
		final int pathCount = sysObject.getFolderIdCount();
		for (int i = 0; i < pathCount; i++) {
			IDfId folderId = sysObject.getFolderId(i);
			IDfFolder parent = session.getFolderBySpecification(folderId.getId());
			req.add(parent);
		}
		return req;
	}
}