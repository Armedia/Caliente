/**
 *
 */

package com.armedia.cmf.engine.documentum.exporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import com.armedia.cmf.engine.converter.IntermediateProperty;
import com.armedia.cmf.engine.documentum.DctmAttributes;
import com.armedia.cmf.engine.documentum.DctmDataType;
import com.armedia.cmf.engine.documentum.DctmException;
import com.armedia.cmf.engine.documentum.DctmMappingUtils;
import com.armedia.cmf.engine.documentum.DctmVersionNumber;
import com.armedia.cmf.engine.documentum.DctmVersionTree;
import com.armedia.cmf.engine.documentum.DfUtils;
import com.armedia.cmf.engine.documentum.DfValueFactory;
import com.armedia.cmf.engine.documentum.common.DctmSysObject;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfObjectRef;
import com.armedia.cmf.storage.CmfProperty;
import com.armedia.cmf.storage.CmfType;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.DfIdNotFoundException;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.IDfType;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.client.content.IDfStore;
import com.documentum.fc.client.distributed.IDfReference;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.DfTime;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfTime;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class DctmExportSysObject<T extends IDfSysObject> extends DctmExportDelegate<T> implements DctmSysObject {

	protected static final class Version<T extends IDfSysObject> implements Comparable<Version<T>> {
		public final T object;
		private final IDfId id;
		private final IDfTime creationDate;
		public final DctmVersionNumber versionNumber;

		private Version(DctmVersionNumber versionNumber, T object) throws DfException {
			this.versionNumber = versionNumber;
			this.object = object;
			this.id = object.getObjectId();
			this.creationDate = Tools.coalesce(object.getCreationDate(), DfTime.DF_NULLDATE);
		}

		@Override
		public int compareTo(Version<T> o) {
			if (o == null) { return 1; }
			if (equals(o)) { return 0; }

			// If they're siblings, resort to version numbering
			if (this.versionNumber.isSibling(o.versionNumber)) { return this.versionNumber.compareTo(o.versionNumber); }

			// First, check hierarchy
			if (this.versionNumber.isAntecedentOf(o.versionNumber)) { return -1; }
			if (this.versionNumber.isSuccessorOf(o.versionNumber)) { return 1; }
			if (this.versionNumber.isAncestorOf(o.versionNumber)) { return -1; }
			if (this.versionNumber.isDescendantOf(o.versionNumber)) { return 1; }

			// if there is no familial (hierarchy or peer) relationship, so fall back to chronology
			final int dateResult = this.creationDate.compareTo(o.creationDate);
			if (dateResult != 0) { return dateResult; }

			// No familial or temporal relationship...so can't establish an order between them...
			// sort by whomever's version number is "earliest"
			return this.versionNumber.compareTo(o.versionNumber);
		}

		@Override
		public int hashCode() {
			return Tools.hashTool(this, null, this.versionNumber);
		}

		@Override
		public boolean equals(Object obj) {
			if (!Tools.baseEquals(this, obj)) { return false; }
			@SuppressWarnings("unchecked")
			Version<T> other = (Version<T>) obj;
			return (Tools.compare(this.versionNumber, other.versionNumber) == 0);
		}

		@Override
		public String toString() {
			return String.format("Version [object=%s, creationDate=%s, versionNumber=%s]", this.id,
				DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(this.creationDate.getDate()), this.versionNumber);
		}
	}

	protected interface RecursionCalculator {
		public String getValue(IDfSysObject o) throws DfException;
	}

	private static final String CTX_VERSION_HISTORY = "VERSION_HISTORY_%S";
	private static final String CTX_VERSION_PATCHES = "VERSION_PATCHES_%S";
	private static final String CTX_VERSION_INDEXES = "VERSION_INDEXES_%S";
	private static final String CTX_VERSION_CURRENT = "VERSION_CURRENT_%S";
	private static final String CTX_PATCH_ANTECEDENT = "PATCH_ANTECEDENT_%S";

	private static final String HISTORY_PATH_IDS = "HISTORY_PATH_IDS_%S";
	private static final String HISTORY_VDOC_STATUS = "HISTORY_VDOC_STATUS_%S";

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

	protected final List<List<String>> calculateRecursions(final IDfSysObject f, final Set<String> visited,
		final RecursionCalculator calc) throws DfException {
		final String oid = f.getObjectId().getId();
		if ((visited != null) && !visited
			.add(oid)) { throw new DfException(String.format("Visited node [%s] twice (history = %s)", oid, visited)); }
		IDfSession session = f.getSession();
		final int parentCount = f.getFolderIdCount();
		List<List<String>> all = new ArrayList<List<String>>(parentCount);
		for (int i = 0; i < parentCount; i++) {
			final IDfId parentId = f.getFolderId(i);
			final IDfSysObject parent = IDfSysObject.class.cast(session.getObject(parentId));
			for (List<String> l : calculateRecursions(parent, visited, calc)) {
				l.add(calc.getValue(parent));
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
		return calculateRecursions(f, new LinkedHashSet<String>(), new RecursionCalculator() {
			@Override
			public String getValue(IDfSysObject o) throws DfException {
				return o.getObjectName();
			}
		});
	}

	protected final List<List<String>> calculateAllParentIds(IDfSysObject f) throws DfException {
		return calculateRecursions(f, new LinkedHashSet<String>(), new RecursionCalculator() {
			@Override
			public String getValue(IDfSysObject o) throws DfException {
				return o.getChronicleId().getId();
			}
		});
	}

	@Override
	protected boolean getDataProperties(DctmExportContext ctx, Collection<CmfProperty<IDfValue>> properties, T object)
		throws DfException, ExportException {
		if (!super.getDataProperties(ctx, properties, object)) { return false; }

		IDfSession session = object.getSession();

		CmfProperty<IDfValue> aclInheritedProp = new CmfProperty<IDfValue>(IntermediateProperty.ACL_INHERITANCE,
			DctmDataType.DF_STRING.getStoredType(), false);
		properties.add(aclInheritedProp);
		boolean aclInheritedSet = false;
		final IDfACL acl = object.getACL();

		CmfProperty<IDfValue> paths = new CmfProperty<IDfValue>(IntermediateProperty.PATH,
			DctmDataType.DF_STRING.getStoredType(), true);
		properties.add(paths);
		CmfProperty<IDfValue> parents = new CmfProperty<IDfValue>(IntermediateProperty.PARENT_ID,
			DctmDataType.DF_ID.getStoredType(), true);
		properties.add(parents);

		final int parentCount = object.getValueCount(DctmAttributes.I_FOLDER_ID);
		for (int i = 0; i < parentCount; i++) {
			final IDfValue folderId = object.getRepeatingValue(DctmAttributes.I_FOLDER_ID, i);
			final IDfFolder parent;
			try {
				parent = session.getFolderBySpecification(folderId.asId().getId());
				if (!aclInheritedSet) {
					// Is the object's ACL the same as its parent folder's?
					IDfACL parentACL = parent.getACL();
					if (Tools.equals(parentACL.getObjectId(), acl.getObjectId())) {
						aclInheritedProp
							.setValue(DfValueFactory.newStringValue(String.format("FOLDER[%s]", folderId.asString())));
						aclInheritedSet = true;
					}
				}
			} catch (DfIdNotFoundException e) {
				this.log
					.warn(String.format("%s [%s](%s) references non-existent folder [%s]", object.getType().getName(),
						object.getObjectName(), object.getObjectId().getId(), folderId.asString()));
				continue;
			}
			parents.addValue(folderId);
			final int pathCount = parent.getFolderPathCount();
			for (int p = 0; p < pathCount; p++) {
				paths.addValue(DfValueFactory.newStringValue(parent.getFolderPath(p)));
			}
		}

		if (!aclInheritedSet) {
			IDfUser creator = session.getUser(object.getCreatorName());
			if ((creator != null) && Tools.equals(acl.getDomain(), creator.getACLDomain())
				&& Tools.equals(acl.getObjectName(), creator.getACLName())) {
				// Make sure we perform all necessary mappings
				String userName = DctmMappingUtils.substituteMappableUsers(session, creator.getUserName());
				aclInheritedProp.setValue(DfValueFactory.newStringValue(String.format("USER[%s]", userName)));
				aclInheritedSet = true;
			}
		}

		if (!aclInheritedSet) {
			IDfType type = object.getType();
			// Need to scale up the type hierarchy...
			while (type != null) {
				IDfCollection c = null;
				try {
					String dql = String.format("select acl_domain, acl_name from dmi_type_info where r_type_id = %s",
						DfUtils.quoteString(type.getObjectId().getId()));
					c = DfUtils.executeQuery(session, dql);
					if (c.next()) {
						String aclDomain = c.getString(DctmAttributes.ACL_DOMAIN);
						String aclName = c.getString(DctmAttributes.ACL_NAME);
						if (Tools.equals(acl.getDomain(), aclDomain) && Tools.equals(acl.getObjectName(), aclName)) {
							aclInheritedProp
								.setValue(DfValueFactory.newStringValue(String.format("TYPE[%s]", type.getName())));
							aclInheritedSet = true;
							break;
						}
					}
				} finally {
					DfUtils.closeQuietly(c);
				}

				type = type.getSuperType();
			}
		}

		if (!aclInheritedSet) {
			aclInheritedProp.setValue(DfValueFactory.newStringValue("NONE[]"));
			aclInheritedSet = true;
		}

		IDfReference ref = getReferenceFor(object);
		if (ref != null) {
			properties
				.add(new CmfProperty<IDfValue>(DctmAttributes.BINDING_CONDITION, DctmDataType.DF_STRING.getStoredType(),
					false, DfValueFactory.newStringValue(ref.getBindingCondition())));
			properties.add(new CmfProperty<IDfValue>(DctmAttributes.BINDING_LABEL,
				DctmDataType.DF_STRING.getStoredType(), false, DfValueFactory.newStringValue(ref.getBindingLabel())));
			properties
				.add(new CmfProperty<IDfValue>(DctmAttributes.LOCAL_FOLDER_LINK, DctmDataType.DF_STRING.getStoredType(),
					false, DfValueFactory.newStringValue(ref.getLocalFolderLink())));
			properties
				.add(new CmfProperty<IDfValue>(DctmAttributes.REFERENCE_DB_NAME, DctmDataType.DF_STRING.getStoredType(),
					false, DfValueFactory.newStringValue(ref.getReferenceDbName())));
			properties.add(new CmfProperty<IDfValue>(DctmAttributes.REFERENCE_BY_ID, DctmDataType.DF_ID.getStoredType(),
				false, DfValueFactory.newIdValue(ref.getReferenceById())));
			properties
				.add(new CmfProperty<IDfValue>(DctmAttributes.REFERENCE_BY_NAME, DctmDataType.DF_STRING.getStoredType(),
					false, DfValueFactory.newStringValue(ref.getReferenceByName())));
			properties.add(new CmfProperty<IDfValue>(DctmAttributes.REFRESH_INTERVAL,
				DctmDataType.DF_INTEGER.getStoredType(), false, DfValueFactory.newIntValue(ref.getRefreshInterval())));

			properties.add(new CmfProperty<IDfValue>(IntermediateProperty.VERSION_COUNT,
				DctmDataType.DF_INTEGER.getStoredType(), false, DfValueFactory.newIntValue(1)));
			properties.add(new CmfProperty<IDfValue>(IntermediateProperty.VERSION_INDEX,
				DctmDataType.DF_INTEGER.getStoredType(), false, DfValueFactory.newIntValue(1)));
			properties.add(new CmfProperty<IDfValue>(IntermediateProperty.VERSION_HEAD_INDEX,
				DctmDataType.DF_INTEGER.getStoredType(), false, DfValueFactory.newIntValue(1)));
			return false;
		}

		CmfProperty<IDfValue> aclIdProp = new CmfProperty<IDfValue>(IntermediateProperty.ACL_ID,
			DctmDataType.DF_ID.getStoredType(), false);
		properties.add(aclIdProp);
		IDfId aclId = DfId.DF_NULLID;
		IDfACL aclObj = object.getACL();
		if (aclObj != null) {
			aclId = aclObj.getObjectId();
		}
		aclIdProp.setValue(DfValueFactory.newIdValue(aclId));
		return true;
	}

	protected Set<String> calculateParentTreeIds(T object) throws DfException {
		Set<String> ptid = new LinkedHashSet<String>();
		final int parentCount = object.getValueCount(DctmAttributes.I_FOLDER_ID);
		for (int i = 0; i < parentCount; i++) {
			final IDfValue folderId = this.object.getRepeatingValue(DctmAttributes.I_FOLDER_ID, i);
			Set<String> parentIdPaths = this.factory.pathIdCache.get(folderId.asString());
			if ((parentIdPaths != null) && !parentIdPaths.isEmpty()) {
				for (String s : parentIdPaths) {
					ptid.add(String.format("%s/%s", s, folderId.asString()));
				}
			} else {
				ptid.add(folderId.asString());
			}
		}
		return ptid;
	}

	@Override
	protected void prepareForStorage(DctmExportContext ctx, CmfObject<IDfValue> marshaled, T object)
		throws ExportException, DfException {
		final String chronicleId = object.getChronicleId().getId();
		CmfProperty<IDfValue> prop = new CmfProperty<IDfValue>(IntermediateProperty.PARENT_TREE_IDS,
			DctmDataType.DF_STRING.getStoredType(), true);
		marshaled.setProperty(prop);
		Set<String> ptid = calculateParentTreeIds(object);
		for (String s : ptid) {
			prop.addValue(DfValueFactory.newStringValue(s));
		}
		this.factory.pathIdCache.put(object.getObjectId().getId(), Collections.unmodifiableSet(ptid));

		String marker = String.format(DctmExportSysObject.HISTORY_PATH_IDS, chronicleId);
		ptid = ctx.getObject(marker);
		if (ptid != null) {
			prop = new CmfProperty<IDfValue>(IntermediateProperty.LATEST_PARENT_TREE_IDS,
				DctmDataType.DF_STRING.getStoredType(), true);
			marshaled.setProperty(prop);
			for (String s : ptid) {
				prop.addValue(DfValueFactory.newStringValue(s));
			}
		}
		marker = String.format(DctmExportSysObject.HISTORY_VDOC_STATUS, chronicleId);
		Boolean vdocMarker = ctx.getObject(marker);
		if (vdocMarker == null) {
			IDfCollection c = null;
			try {
				String dql = String.format(
					"select count(*) as vdocs from dm_document (ALL) where i_chronicle_id = %s and ((r_is_virtual_doc = 1) or (r_link_cnt > 0))",
					DfUtils.quoteString(chronicleId));
				c = DfUtils.executeQuery(ctx.getSession(), dql);
				vdocMarker = c.next() && (c.getInt("vdocs") > 0);
				ctx.setObject(marker, vdocMarker);
			} finally {
				DfUtils.closeQuietly(c);
			}
		}
		prop = new CmfProperty<IDfValue>(IntermediateProperty.VDOC_HISTORY, DctmDataType.DF_BOOLEAN.getStoredType(),
			false);
		marshaled.setProperty(prop);
		prop.setValue(DfValueFactory.newBooleanValue(vdocMarker.booleanValue()));
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
	protected final List<Version<T>> getVersionHistory(DctmExportContext ctx, T object)
		throws DfException, ExportException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object whose versions to analyze"); }
		final IDfSession session = object.getSession();
		final IDfId chronicleId = object.getChronicleId();
		final String historyObject = String.format(DctmExportSysObject.CTX_VERSION_HISTORY, chronicleId.getId());

		List<Version<T>> history = ctx.getObject(historyObject);
		if (history != null) { return history; }

		T currentObject = null;

		// No existing history, we must calculate it
		history = new LinkedList<Version<T>>();
		final DctmVersionTree tree;
		try {
			tree = new DctmVersionTree(session, chronicleId);
		} catch (DctmException e) {
			throw new ExportException(
				String.format("Failed to obtain the version tree for chronicle [%s]", chronicleId.getId()), e);
		}
		List<IDfValue> patches = new ArrayList<IDfValue>();
		for (DctmVersionNumber versionNumber : tree.allVersions) {
			if (tree.totalPatches.contains(versionNumber)) {
				patches.add(DfValueFactory.newStringValue(versionNumber.toString()));
				continue;
			}

			final IDfId id = new DfId(tree.indexByVersionNumber.get(versionNumber));
			final T obj = castObject(session.getObject(id));
			if (obj.getHasFolder()) {
				currentObject = obj;
			}
			final Version<T> entry = new Version<T>(versionNumber, obj);
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

		// We need to sort the version history if branches are involved, since all versions must be
		// in proper chronological order. Relative order is not enough...this is only necessary
		// if branches are involved because "cousins" may not be properly ordered chronologically
		// and this needs to be the case in order to support import by other systems.
		if (tree.isBranched()) {
			Collections.sort(history);
		}

		if (this.log.isTraceEnabled()) {
			// Make a list of the versions in play
			int i = 0;
			for (Version<T> v : history) {
				String msg = String.format("HISTORY: %s[%02d]: %s", chronicleId, i++, v);
				ctx.printf(msg);
				this.log.trace(msg);
			}
		}

		// Only put this in the context when it's needed
		history = Tools.freezeList(history);
		ctx.setObject(historyObject, history);

		// Now, we make sure we write down the index position for each version within the history
		String indexesName = String.format(DctmExportSysObject.CTX_VERSION_INDEXES, chronicleId.getId());
		Map<String, Integer> m = new HashMap<String, Integer>(history.size());
		int i = 0;
		for (Version<T> v : history) {
			m.put(v.id.getId(), ++i);
		}
		m = Tools.freezeMap(m);
		ctx.setObject(indexesName, m);

		if (currentObject != null) {
			String markerName = String.format(DctmExportSysObject.CTX_VERSION_CURRENT, chronicleId.getId());
			ctx.setObject(markerName, currentObject.getObjectId().getId());
		}

		return history;
	}

	@Override
	protected void requirementsExported(CmfObject<IDfValue> marshalled, DctmExportContext ctx) throws Exception {
		T currentObject = castObject(ctx.getSession().getObject(new DfId(marshalled.getId())));
		IDfId chronicleId = currentObject.getChronicleId();
		String markerName = String.format(DctmExportSysObject.HISTORY_PATH_IDS, chronicleId.getId());
		ctx.setObject(markerName, calculateParentTreeIds(currentObject));
	}

	protected final List<IDfValue> getVersionPatches(T object, DctmExportContext ctx) throws DfException {
		return ctx.getObject(String.format(DctmExportSysObject.CTX_VERSION_PATCHES, object.getObjectId().getId()));
	}

	protected final Integer getVersionIndex(T object, DctmExportContext ctx) throws DfException {
		Map<String, Integer> m = ctx
			.getObject(String.format(DctmExportSysObject.CTX_VERSION_INDEXES, object.getChronicleId().getId()));
		if (m == null) { return null; }
		return m.get(object.getObjectId().getId());
	}

	protected final Integer getHeadIndex(T object, DctmExportContext ctx) throws DfException, ExportException {
		IDfId chronicleId = object.getChronicleId();
		String currentId = ctx.getObject(String.format(DctmExportSysObject.CTX_VERSION_CURRENT, chronicleId.getId()));
		if (currentId == null) { return null; }

		Map<String, Integer> m = ctx
			.getObject(String.format(DctmExportSysObject.CTX_VERSION_INDEXES, object.getChronicleId().getId()));
		if (m == null) { return null; }
		return m.get(currentId);
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
				String path = (f.getFolderPathCount() > 0 ? f.getFolderPath(0)
					: String.format("(unknown-folder:[%s])", id.getId()));
				return String.format("%s/%s [%s]", path, objectName, calculateVersionString(sysObject, true));
			}
		}
		throw new DfException(
			String.format("None of the parent paths for object [%s] were found", sysObject.getObjectId().getId()));
	}

	@Override
	protected String calculateBatchId(T object) throws Exception {
		return object.getChronicleId().getId();
	}

	protected boolean isDfReference(T object) throws DfException {
		return object.isReference();
	}

	@Override
	protected Collection<DctmExportDelegate<?>> findRequirements(IDfSession session, CmfObject<IDfValue> marshaled,
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

		IDfReference ref = getReferenceFor(sysObject);
		if (ref != null) {
			IDfId referrentId = ref.getReferenceById();
			if (Tools.equals(referrentId.getDocbaseId(), sysObject.getObjectId().getDocbaseId())) {
				// The object is from the same docbase, so we make sure it's listed as a requirement
				// to ensure it gets copied AFTER the referrent gets copied
				DctmExportDelegate<?> delegate = this.factory.newExportDelegate(session.getObject(referrentId));
				if (delegate == null) { throw new ExportException(String.format(
					"The %s [%s](%s) is a reference to an object with ID (%s), but that object is not supported to be exported",
					marshaled.getType(), marshaled.getLabel(), marshaled.getId(), referrentId.getId())); }
				req.add(delegate);
			}

			// For references, we stop here
			return req;
		}

		// Export the ACL requirements
		req.add(this.factory.newExportDelegate(sysObject.getACL()));
		return req;
	}

	protected IDfReference getReferenceFor(T object) throws DfException {
		if ((object == null) || !object.isReference()) { return null; }
		IDfCollection c = null;
		final IDfSession s = object.getSession();
		try {
			c = DfUtils.executeQuery(s,
				String.format("select r_object_id from dm_reference_s where r_mirror_object_id = %s",
					DfUtils.quoteString(object.getObjectId().getId())));
			if (!c.next()) { return null; }
			return IDfReference.class.cast(s.getObject(c.getId("r_object_id")));
		} finally {
			DfUtils.closeQuietly(c);
		}
	}

	@Override
	protected String calculateName(T sysObject) throws Exception {
		return sysObject.getObjectName();
	}

	@Override
	protected Collection<CmfObjectRef> calculateParentIds(T sysObject) throws Exception {
		List<CmfObjectRef> ret = new ArrayList<CmfObjectRef>();
		for (IDfValue v : DfValueFactory.getAllRepeatingValues(DctmAttributes.I_FOLDER_ID, sysObject)) {
			ret.add(new CmfObjectRef(CmfType.FOLDER, v.asId().getId()));
		}
		return ret;
	}

	@Override
	protected boolean calculateBatchHead(T sysObject) throws Exception {
		return sysObject.getHasFolder();
	}
}