/**
 *
 */

package com.armedia.cmf.engine.documentum.exporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.armedia.cmf.engine.converter.IntermediateProperty;
import com.armedia.cmf.engine.documentum.DctmAttributes;
import com.armedia.cmf.engine.documentum.DctmDataType;
import com.armedia.cmf.engine.documentum.DctmException;
import com.armedia.cmf.engine.documentum.DctmVersionNumber;
import com.armedia.cmf.engine.documentum.DctmVersionTree;
import com.armedia.cmf.engine.documentum.DfUtils;
import com.armedia.cmf.engine.documentum.DfValueFactory;
import com.armedia.cmf.engine.documentum.common.DctmSysObject;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredProperty;
import com.armedia.cmf.storage.tools.FilenameFixer;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.content.IDfStore;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class DctmExportSysObject<T extends IDfSysObject> extends DctmExportDelegate<T> implements DctmSysObject {

	private static final String CTX_VERSION_HISTORY = "VERSION_HISTORY_%S";
	private static final String CTX_VERSION_PATCHES = "VERSION_PATCHES_%S";
	private static final String CTX_PATCH_ANTECEDENT = "PATCH_ANTECEDENT_%S";

	protected DctmExportSysObject(DctmExportDelegateFactory factory, Class<T> objectClass, T object) throws Exception {
		super(factory, objectClass, object);
	}

	protected List<String> calculateFullPath(IDfSysObject f) throws DfException {
		IDfSession session = f.getSession();
		LinkedList<String> ret = new LinkedList<String>();
		while (f != null) {
			ret.addFirst(f.getObjectName());
			if (f.getValueCount(DctmAttributes.I_FOLDER_ID) < 1) {
				break;
			}
			f = IDfSysObject.class.cast(session.getFolderBySpecification(f.getString(DctmAttributes.I_FOLDER_ID)));
		}
		return ret;
	}

	protected final List<List<String>> calculateAllPaths(final IDfSysObject f, final Set<String> visited)
		throws DfException {
		final String oid = f.getObjectId().getId();
		if ((visited != null) && !visited.add(oid)) { throw new DfException(String.format(
			"Visited node [%s] twice (history = %s)", oid, visited)); }
		IDfSession session = f.getSession();
		final int parentCount = f.getFolderIdCount();
		List<List<String>> all = new ArrayList<List<String>>(parentCount);
		for (int i = 0; i < parentCount; i++) {
			final IDfId parentId = f.getFolderId(i);
			final IDfSysObject parent = IDfSysObject.class.cast(session.getObject(parentId));
			for (List<String> l : calculateAllPaths(parent, visited)) {
				l.add(parent.getObjectName());
				all.add(l);
			}
		}
		if (all.isEmpty()) {
			// No parents...so we need to add a single path with ${object_name}
			all.add(new ArrayList<String>(1));
		}
		if (visited != null) {
			visited.remove(oid);
		}
		return all;
	}

	protected final List<List<String>> calculateAllPaths(IDfSysObject f) throws DfException {
		return calculateAllPaths(f, new LinkedHashSet<String>());
	}

	@Override
	protected void getDataProperties(DctmExportContext ctx, Collection<StoredProperty<IDfValue>> properties, T object)
		throws DfException, ExportException {
		StoredProperty<IDfValue> paths = new StoredProperty<IDfValue>(IntermediateProperty.PATH.encode(),
			DctmDataType.DF_STRING.getStoredType(), true);
		StoredProperty<IDfValue> pathsEncoded = new StoredProperty<IDfValue>(
			IntermediateProperty.PATH_ENCODED.encode(), DctmDataType.DF_BOOLEAN.getStoredType(), true);
		StringBuilder sb = new StringBuilder();
		for (List<String> l : calculateAllPaths(object)) {
			sb.setLength(0);
			boolean encoded = false;
			for (String s : l) {
				String str = FilenameFixer.urlEncode(s);
				sb.append('/').append(str);
				encoded |= !Tools.equals(str, s);
			}
			paths.addValue(DfValueFactory.newStringValue(sb.toString()));
			pathsEncoded.addValue(DfValueFactory.newBooleanValue(encoded));
		}
		properties.add(paths);
		properties.add(pathsEncoded);

		StoredProperty<IDfValue> parents = new StoredProperty<IDfValue>(IntermediateProperty.PARENT_ID.encode(),
			DctmDataType.DF_ID.getStoredType(), true);
		final int parentCount = object.getValueCount(DctmAttributes.I_FOLDER_ID);
		for (int i = 0; i < parentCount; i++) {
			final IDfValue folderId = object.getRepeatingValue(DctmAttributes.I_FOLDER_ID, i);
			parents.addValue(folderId);
		}
		properties.add(parents);
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
	protected final List<T> getVersionHistory(DctmExportContext ctx, T object) throws DfException, ExportException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object whose versions to analyze"); }
		final IDfSession session = object.getSession();
		final IDfId chronicleId = object.getChronicleId();
		final String historyObject = String.format(DctmExportSysObject.CTX_VERSION_HISTORY, chronicleId.toString());

		@SuppressWarnings("unchecked")
		List<T> history = (List<T>) ctx.getObject(historyObject);
		if (history != null) { return history; }

		// No existing history, we must calculate it
		history = new LinkedList<T>();
		final DctmVersionTree tree;
		try {
			tree = new DctmVersionTree(session, chronicleId);
		} catch (DctmException e) {
			throw new ExportException(String.format("Failed to obtain the version tree for chronicle [%s]",
				chronicleId.getId()), e);
		}
		List<IDfValue> patches = new ArrayList<IDfValue>();
		for (DctmVersionNumber versionNumber : tree.allVersions) {
			if (tree.totalPatches.contains(versionNumber)) {
				patches.add(DfValueFactory.newStringValue(versionNumber.toString()));
				continue;
			}

			final IDfId id = new DfId(tree.indexByVersionNumber.get(versionNumber));
			final T entry = castObject(session.getObject(id));
			history.add(entry);
			if (!patches.isEmpty()) {
				patches = Tools.freezeList(patches);
				final String patchesObject = String.format(DctmExportSysObject.CTX_VERSION_PATCHES, id.getId());
				ctx.setObject(patchesObject, patches);
				patches = new ArrayList<IDfValue>();
			}

			DctmVersionNumber alternateAntecedent = tree.alternateAntecedent.get(versionNumber);
			if (alternateAntecedent != null) {
				String antecedentId = tree.indexByVersionNumber.get(alternateAntecedent);
				if (antecedentId == null) {
					antecedentId = alternateAntecedent.toString();
				}
				ctx.setValue(String.format(DctmExportSysObject.CTX_PATCH_ANTECEDENT, id.getId()),
					DfValueFactory.newStringValue(antecedentId));
			}
		}
		// Only put this in the context when it's needed
		history = Tools.freezeList(history);
		ctx.setObject(historyObject, history);
		return history;
	}

	protected final List<IDfValue> getVersionPatches(T object, DctmExportContext ctx) throws DfException {
		Object o = ctx.getObject(String.format(DctmExportSysObject.CTX_VERSION_PATCHES, object.getObjectId().getId()));
		if (o == null) { return null; }
		@SuppressWarnings("unchecked")
		List<IDfValue> l = (List<IDfValue>) o;
		return l;
	}

	protected final IDfValue getPatchAntecedent(T object, DctmExportContext ctx) throws DfException {
		return ctx.getValue(String.format(DctmExportSysObject.CTX_PATCH_ANTECEDENT, object.getObjectId().getId()));
	}

	@Override
	protected String calculateLabel(T sysObject) throws Exception {
		final IDfSession session = sysObject.getSession();
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

	@Override
	protected String calculateBatchId(T object) throws Exception {
		return object.getChronicleId().getId();
	}

	protected boolean isDfReference(T object) throws DfException {
		// TODO: No reference support...yet...uncomment this when testing
		// the reference support
		// return object.isReference();
		return false;
	}

	@Override
	protected Collection<DctmExportDelegate<?>> findRequirements(IDfSession session, StoredObject<IDfValue> marshaled,
		T sysObject, DctmExportContext ctx) throws Exception {
		Collection<DctmExportDelegate<?>> req = super.findRequirements(session, marshaled, sysObject, ctx);

		// The parent folders
		final int pathCount = sysObject.getFolderIdCount();
		for (int i = 0; i < pathCount; i++) {
			IDfId folderId = sysObject.getFolderId(i);
			IDfFolder parent = session.getFolderBySpecification(folderId.getId());
			req.add(this.factory.newExportDelegate(parent));
		}

		// We export our filestore
		String storeName = sysObject.getStorageType();
		if (StringUtils.isNotBlank(storeName)) {
			IDfStore store = DfUtils.getStore(session, storeName);
			if (store != null) {
				req.add(this.factory.newExportDelegate(store));
			} else {
				this.log.warn("SysObject {} refers to missing store [{}]", marshaled.getLabel(), storeName);
			}
		}
		return req;
	}
}