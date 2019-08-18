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

package com.armedia.caliente.engine.dfc.exporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.dfc.DctmAttributes;
import com.armedia.caliente.engine.dfc.DctmDataType;
import com.armedia.caliente.engine.dfc.DctmMappingUtils;
import com.armedia.caliente.engine.dfc.common.DctmSysObject;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectRef;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.tools.dfc.DctmException;
import com.armedia.caliente.tools.dfc.DfValueFactory;
import com.armedia.caliente.tools.dfc.DfcQuery;
import com.armedia.caliente.tools.dfc.DfcUtils;
import com.armedia.caliente.tools.dfc.DfcVersionHistory;
import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.function.CheckedFunction;
import com.documentum.fc.client.DfIdNotFoundException;
import com.documentum.fc.client.DfObjectNotFoundException;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfFormat;
import com.documentum.fc.client.IDfGroup;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.IDfType;
import com.documentum.fc.client.IDfTypedObject;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.client.IDfVirtualDocument;
import com.documentum.fc.client.IDfVirtualDocumentNode;
import com.documentum.fc.client.content.IDfStore;
import com.documentum.fc.client.distributed.IDfReference;
import com.documentum.fc.client.impl.ISysObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfValue;

/**
 *
 *
 */
public class DctmExportSysObject<T extends IDfSysObject> extends DctmExportDelegate<T> implements DctmSysObject {

	private static final String CTX_VERSION_HISTORY = "VERSION_HISTORY_%S";

	private static final String HISTORY_PATH_IDS = "HISTORY_PATH_IDS_%S";
	private static final String HISTORY_VDOC_STATUS = "HISTORY_VDOC_STATUS_%S";

	protected DctmExportSysObject(DctmExportDelegateFactory factory, IDfSession session, Class<T> objectClass, T object)
		throws Exception {
		super(factory, session, objectClass, object);
	}

	@Override
	protected Set<String> calculateSecondarySubtypes(IDfSession session, CmfObject.Archetype type, String subtype,
		T object) throws Exception {
		Set<String> secondaries = super.calculateSecondarySubtypes(session, type, subtype, object);
		if (object.hasAttr(DctmAttributes.R_ASPECT_NAME)) {
			final int count = object.getValueCount(DctmAttributes.R_ASPECT_NAME);
			for (int i = 0; i < count; i++) {
				secondaries.add(object.getRepeatingString(DctmAttributes.R_ASPECT_NAME, i));
			}
		}
		return secondaries;
	}

	private final List<List<String>> calculateRecursions(final IDfSysObject f, Set<String> visited,
		final CheckedFunction<IDfSysObject, String, DfException> calc) throws DfException {
		final String oid = f.getObjectId().getId();
		if (visited == null) {
			visited = new LinkedHashSet<>();
		}
		if (!visited.add(oid)) {
			throw new DfException(String.format("Visited node [%s] twice (history = %s)", oid, visited));
		}
		IDfSession session = f.getSession();
		final int parentCount = f.getFolderIdCount();
		List<List<String>> all = new ArrayList<>(parentCount);
		for (int i = 0; i < parentCount; i++) {
			final IDfId parentId = f.getFolderId(i);

			// Validate that it's a valid ID...
			if (parentId.isNull() || !parentId.isObjectId()) {
				this.log.warn("Invalid parent ID [{}] read from object [{}]: [{}]", parentId.toString(), oid);
				continue;
			}

			// Retrieve the parent...
			final IDfFolder parent;
			try {
				parent = session.getFolderBySpecification(parentId.getId());
			} catch (RuntimeException e) {
				// This is a precaution, to catch unchecked exceptions due to DFC bugs...
				throw new DfException(
					String.format("DFC NPE Bug triggered by folder with ID [%s], which is a parent of object [%s]",
						parentId.toString(), oid));
			}

			// Parent not found...?!?!?!
			if (parent == null) { throw new DfIdNotFoundException(parentId); }

			// Recurse upwards!
			for (List<String> l : calculateRecursions(parent, visited, calc)) {
				l.add(calc.apply(parent));
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

	protected final List<List<String>> calculateRecursions(final IDfSysObject f,
		final CheckedFunction<IDfSysObject, String, DfException> calc) throws DfException {
		return calculateRecursions(f, calc);
	}

	protected final List<List<String>> calculateAllPaths(IDfSession session, IDfSysObject f) throws DfException {
		return calculateRecursions(f, null, (o) -> o.getObjectName());

		/*
		final String oid = f.getObjectId().getId();
		final int parentCount = f.getFolderIdCount();
		List<List<String>> ret = new ArrayList<>();
		for (int i = 0; i < parentCount; i++) {
			final IDfId parentId = f.getFolderId(i);
		
			// Validate that it's a valid ID...
			if (parentId.isNull() || !parentId.isObjectId()) {
				this.log.warn("Invalid parent ID [{}] read from object [{}]: [{}]", parentId.toString(), oid);
				continue;
			}
		
			// Retrieve the parent...
			final IDfFolder parent;
			try {
				parent = session.getFolderBySpecification(parentId.getId());
			} catch (RuntimeException e) {
				// This is a precaution, to catch unchecked exceptions due to DFC bugs...
				throw new DfException(
					String.format("DFC NPE Bug triggered by folder with ID [%s], which is a parent of object [%s]",
						parentId.toString(), oid));
			}
		
			// Parent not found...?!?!?!
			if (parent == null) { throw new DfIdNotFoundException(parentId); }
		
			final int pathCount = parent.getFolderPathCount();
			for (int j = 0; j < pathCount; j++) {
				ret.add(FileNameTools.tokenize(parent.getFolderPath(j), '/'));
			}
		}
		return ret;
		*/
	}

	protected final List<List<String>> calculateAllParentIds(IDfSysObject f) throws DfException {
		return calculateRecursions(f, null, (o) -> o.getChronicleId().getId());
	}

	protected final boolean isSameACL(T object, IDfTypedObject other) throws DfException {
		if (IDfSysObject.class.isInstance(other)) { return isSameACL(object, IDfSysObject.class.cast(other)); }
		return isSameACL(object, other, null, null);
	}

	protected final boolean isSameACL(T object, IDfTypedObject other, String aclDomainAtt, String aclNameAtt)
		throws DfException {
		if (object == other) { return true; }
		if ((object == null) || (other == null)) { return false; }
		if (StringUtils.isBlank(aclDomainAtt)) {
			aclDomainAtt = DctmAttributes.ACL_DOMAIN;
		}
		if (!other.hasAttr(aclDomainAtt)) { return false; }
		if (StringUtils.isBlank(aclNameAtt)) {
			aclNameAtt = DctmAttributes.ACL_NAME;
		}
		if (!other.hasAttr(aclNameAtt)) { return false; }
		return isSameACL(object, other.getString(aclDomainAtt), other.getString(aclNameAtt));
	}

	protected final boolean isSameACL(T object, IDfSysObject other) throws DfException {
		if (object == other) { return true; }
		if ((object == null) || (other == null)) { return false; }
		return isSameACL(object, other.getACLDomain(), other.getACLName());
	}

	protected final boolean isSameACL(T object, String aclDomain, String aclName) throws DfException {
		if (!Tools.equals(object.getACLDomain(), aclDomain)) { return false; }
		if (!Tools.equals(object.getACLName(), aclName)) { return false; }
		return true;
	}

	@Override
	protected boolean getDataProperties(DctmExportContext ctx, Collection<CmfProperty<IDfValue>> properties, T object)
		throws DfException, ExportException {
		if (!super.getDataProperties(ctx, properties, object)) { return false; }

		IDfSession session = object.getSession();

		CmfProperty<IDfValue> reference = new CmfProperty<>(IntermediateProperty.IS_REFERENCE,
			DctmDataType.DF_BOOLEAN.getStoredType(), false);
		properties.add(reference);
		reference.setValue(DfValueFactory.of(object.isReference()));

		CmfProperty<IDfValue> aclInheritedProp = new CmfProperty<>(IntermediateProperty.ACL_INHERITANCE,
			DctmDataType.DF_STRING.getStoredType(), false);
		properties.add(aclInheritedProp);
		boolean aclInheritedSet = false;

		CmfProperty<IDfValue> paths = new CmfProperty<>(IntermediateProperty.PATH,
			DctmDataType.DF_STRING.getStoredType(), true);
		properties.add(paths);
		CmfProperty<IDfValue> fullPaths = new CmfProperty<>(IntermediateProperty.FULL_PATH,
			DctmDataType.DF_STRING.getStoredType(), true);
		properties.add(fullPaths);
		CmfProperty<IDfValue> parents = new CmfProperty<>(IntermediateProperty.PARENT_ID,
			DctmDataType.DF_ID.getStoredType(), true);
		properties.add(parents);

		final int parentCount = object.getValueCount(DctmAttributes.I_FOLDER_ID);
		for (int i = 0; i < parentCount; i++) {
			final IDfValue folderId = object.getRepeatingValue(DctmAttributes.I_FOLDER_ID, i);
			final IDfFolder parent = session.getFolderBySpecification(folderId.asId().getId());
			if ((parent == null) && !folderId.asId().isNull()) {
				this.log.warn("{} [{}]({}) references non-existent folder [{}]", object.getType().getName(),
					object.getObjectName(), object.getObjectId().getId(), folderId.asString());
				continue;
			}

			// Is the object's ACL the same as its parent folder's?
			if (!aclInheritedSet && isSameACL(object, parent)) {
				aclInheritedProp.setValue(DfValueFactory.of(String.format("FOLDER[%s]", folderId.asString())));
				aclInheritedSet = true;
			}
			parents.addValue(folderId);
		}

		// Calculate the parent paths in the correct order... r_folder_path may have a different
		// order in some documentum instances
		for (List<String> p : calculateAllPaths(ctx.getSession(), object)) {
			String path = FileNameTools.reconstitute(p, true, false, '/');
			paths.addValue(DfValueFactory.of(path));
			fullPaths.addValue(DfValueFactory.of(String.format("%s/%s", path, object.getObjectName())));
		}

		if (!aclInheritedSet) {
			IDfUser creator = session.getUser(object.getCreatorName());
			if ((creator != null) && isSameACL(object, creator)) {
				// Make sure we perform all necessary mappings
				String userName = DctmMappingUtils.substituteMappableUsers(session, creator.getUserName());
				aclInheritedProp.setValue(DfValueFactory.of(String.format("USER[%s]", userName)));
				aclInheritedSet = true;
			}
		}

		if (!aclInheritedSet) {
			IDfType type = object.getType();
			// Need to scale up the type hierarchy...
			while (type != null) {
				String dql = String.format("select acl_domain, acl_name from dmi_type_info where r_type_id = %s",
					DfcUtils.quoteString(type.getObjectId().getId()));
				try (DfcQuery query = new DfcQuery(session, dql)) {
					if (query.hasNext()) {
						if (isSameACL(object, query.next())) {
							aclInheritedProp.setValue(DfValueFactory.of(String.format("TYPE[%s]", type.getName())));
							aclInheritedSet = true;
							break;
						}
					}
				}
				type = type.getSuperType();
			}
		}

		if (!aclInheritedSet) {
			aclInheritedProp.setValue(DfValueFactory.of("NONE[]"));
			aclInheritedSet = true;
		}

		IDfReference ref = getReferenceFor(object);
		if (ref != null) {
			properties.add(new CmfProperty<>(IntermediateProperty.REF_TARGET, DctmDataType.DF_STRING.getStoredType(),
				false, DfValueFactory.of(object.getChronicleId().getId())));
			String refVersion = ref.getBindingLabel();
			if (StringUtils.equalsIgnoreCase(ISysObject.CURRENT_VERSION_LABEL, refVersion)) {
				refVersion = "HEAD";
			}
			properties.add(new CmfProperty<>(IntermediateProperty.REF_VERSION, DctmDataType.DF_STRING.getStoredType(),
				false, DfValueFactory.of(refVersion)));

			properties.add(new CmfProperty<>(DctmAttributes.BINDING_CONDITION, DctmDataType.DF_STRING.getStoredType(),
				false, DfValueFactory.of(ref.getBindingCondition())));
			properties.add(new CmfProperty<>(DctmAttributes.BINDING_LABEL, DctmDataType.DF_STRING.getStoredType(),
				false, DfValueFactory.of(ref.getBindingLabel())));
			properties.add(new CmfProperty<>(DctmAttributes.LOCAL_FOLDER_LINK, DctmDataType.DF_STRING.getStoredType(),
				false, DfValueFactory.of(ref.getLocalFolderLink())));
			properties.add(new CmfProperty<>(DctmAttributes.REFERENCE_DB_NAME, DctmDataType.DF_STRING.getStoredType(),
				false, DfValueFactory.of(ref.getReferenceDbName())));
			properties.add(new CmfProperty<>(DctmAttributes.REFERENCE_BY_ID, DctmDataType.DF_ID.getStoredType(), false,
				DfValueFactory.of(ref.getReferenceById())));
			properties.add(new CmfProperty<>(DctmAttributes.REFERENCE_BY_NAME, DctmDataType.DF_STRING.getStoredType(),
				false, DfValueFactory.of(ref.getReferenceByName())));
			properties.add(new CmfProperty<>(DctmAttributes.REFRESH_INTERVAL, DctmDataType.DF_INTEGER.getStoredType(),
				false, DfValueFactory.of(ref.getRefreshInterval())));

			properties.add(new CmfProperty<>(IntermediateProperty.VERSION_COUNT,
				DctmDataType.DF_INTEGER.getStoredType(), false, DfValueFactory.of(1)));
			properties.add(new CmfProperty<>(IntermediateProperty.VERSION_INDEX,
				DctmDataType.DF_INTEGER.getStoredType(), false, DfValueFactory.of(1)));
			properties.add(new CmfProperty<>(IntermediateProperty.VERSION_HEAD_INDEX,
				DctmDataType.DF_INTEGER.getStoredType(), false, DfValueFactory.of(1)));
			return false;
		}

		CmfProperty<IDfValue> aclIdProp = new CmfProperty<>(IntermediateProperty.ACL_ID,
			DctmDataType.DF_ID.getStoredType(), false);
		properties.add(aclIdProp);
		IDfId aclId = DfId.DF_NULLID;
		String aclDomain = object.getACLDomain();
		String aclName = object.getACLName();
		final String dql = String.format(
			"select distinct r_object_id from dm_acl where owner_name = %s and object_name = %s",
			DfcUtils.quoteString(aclDomain), DfcUtils.quoteString(aclName));
		try (DfcQuery query = new DfcQuery(session, dql, DfcQuery.Type.DF_READ_QUERY)) {
			if (query.hasNext()) {
				aclId = query.next().getId(DctmAttributes.R_OBJECT_ID);
			}
		}
		aclIdProp.setValue(DfValueFactory.of(aclId));
		return true;
	}

	protected Set<String> calculateParentTreeIds(T object) throws DfException {
		Set<String> ptid = new LinkedHashSet<>();
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
		CmfProperty<IDfValue> prop = new CmfProperty<>(IntermediateProperty.PARENT_TREE_IDS,
			DctmDataType.DF_STRING.getStoredType(), true);
		marshaled.setProperty(prop);
		Set<String> ptid = calculateParentTreeIds(object);
		for (String s : ptid) {
			prop.addValue(DfValueFactory.of(s));
		}
		this.factory.pathIdCache.put(object.getObjectId().getId(), Collections.unmodifiableSet(ptid));

		String marker = String.format(DctmExportSysObject.HISTORY_PATH_IDS, chronicleId);
		ptid = ctx.getObject(marker);
		if (ptid != null) {
			prop = new CmfProperty<>(IntermediateProperty.LATEST_PARENT_TREE_IDS,
				DctmDataType.DF_STRING.getStoredType(), true);
			marshaled.setProperty(prop);
			for (String s : ptid) {
				prop.addValue(DfValueFactory.of(s));
			}
		}
		marker = String.format(DctmExportSysObject.HISTORY_VDOC_STATUS, chronicleId);
		Boolean vdocMarker = ctx.getObject(marker);
		if (vdocMarker == null) {
			String dql = String.format(
				"select count(*) as vdocs from dm_document (ALL) where i_chronicle_id = %s and ((r_is_virtual_doc = 1) or (r_link_cnt > 0))",
				DfcUtils.quoteString(chronicleId));
			try (DfcQuery query = new DfcQuery(ctx.getSession(), dql)) {
				vdocMarker = query.hasNext() && (query.next().getInt("vdocs") > 0);
				ctx.setObject(marker, vdocMarker);
			}
		}
		prop = new CmfProperty<>(IntermediateProperty.VDOC_HISTORY, DctmDataType.DF_BOOLEAN.getStoredType(), false);
		marshaled.setProperty(prop);
		prop.setValue(DfValueFactory.of(vdocMarker.booleanValue()));
	}

	protected final String calculateVersionString(IDfSysObject sysObject, boolean full) throws DfException {
		if (!full) {
			return String.format("%s%s", sysObject.getImplicitVersionLabel(),
				sysObject.getHasFolder() ? String.format(",%s", ISysObject.CURRENT_VERSION_LABEL) : "");
		}
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
	protected final DfcVersionHistory<T> getVersionHistory(DctmExportContext ctx, T object)
		throws DfException, ExportException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object whose versions to analyze"); }
		final IDfSession session = object.getSession();
		final IDfId chronicleId = object.getChronicleId();
		final String historyObject = String.format(DctmExportSysObject.CTX_VERSION_HISTORY, chronicleId.getId());

		DfcVersionHistory<T> history = ctx.getObject(historyObject);
		if (history != null) { return history; }

		try {
			history = new DfcVersionHistory<>(session, chronicleId, this.objectClass);
			ctx.setObject(historyObject, history);
		} catch (DctmException e) {
			throw new ExportException(
				String.format("Failed to obtain the version tree for chronicle [%s]", chronicleId.getId()), e);
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

	@Override
	protected String calculateLabel(IDfSession session, T sysObject) throws Exception {
		final int folderCount = sysObject.getFolderIdCount();
		final String objectName = sysObject.getObjectName();
		for (int i = 0; i < folderCount; i++) {
			IDfId id = sysObject.getFolderId(i);
			IDfFolder f = IDfFolder.class.cast(session.getFolderBySpecification(id.getId()));
			if (f != null) {
				String path = (f.getFolderPathCount() > 0 ? f.getFolderPath(0)
					: String.format("(unknown-folder:[%s])", id.getId()));
				return String.format("%s/%s#%s", path, objectName, calculateVersionString(sysObject, true));
			}
		}
		throw new DfException(
			String.format("None of the parent paths for object [%s] were found", sysObject.getObjectId().getId()));
	}

	protected int calculateDocumentDepth(IDfSysObject object, Set<String> visited) throws DfException {
		if (!object.isVirtualDocument() && (object.getLinkCount() <= 0)) { return 0; }

		if (visited == null) {
			// Allow for easy invocation with only one parameter
			visited = new LinkedHashSet<>();
		}
		final IDfId objectId = object.getObjectId();
		if (!visited.add(objectId.getId())) {
			throw new DfException(String.format("Document reference loop detected, element [%s] exists twice: %s",
				object.getObjectId().getId(), visited.toString()));
		}

		try {
			final IDfSession session = object.getSession();
			final String dql = "select distinct r_object_id from dm_sysobject (ALL) where i_chronicle_id = %s and (r_is_virtual_doc = 1 or r_link_cnt > 0) order by 1 ";
			final IDfId chronicleId = object.getChronicleId();
			Integer depth = null;
			try (
				DfcQuery query = new DfcQuery(session, String.format(dql, DfcUtils.quoteString(chronicleId.getId())))) {
				// Now iterate over all the virtual document entries so we can cascade
				while (query.hasNext()) {
					IDfId id = query.next().getId("r_object_id");
					try {
						IDfSysObject o = IDfSysObject.class.cast(session.getObject(id));

						IDfVirtualDocument vDoc = o.asVirtualDocument(ISysObject.CURRENT_VERSION_LABEL, false);
						final IDfVirtualDocumentNode root = vDoc.getRootNode();
						final int members = root.getChildCount();
						for (int i = 0; i < members; i++) {
							final IDfVirtualDocumentNode child = root.getChild(i);
							int refDepth = calculateDocumentDepth(child.getSelectedObject(), visited);

							// If our depth exceeds that of the deepest object yet, then we take
							// this as the new depth
							if ((depth == null) || (refDepth > depth.intValue())) {
								depth = refDepth;
							}
						}
					} catch (DfObjectNotFoundException e) {
						// WTF?!?!? The object isn't there? Report the corruption!!
						throw new DfException(String.format(
							"Corruption detected: object id [%s] is referenced as part of chronicle [%s], but it couldn't be loaded",
							id.getId(), chronicleId.getId()), e);
					}
				}
			}
			// if we're not virtual documents, our default depth is 0... if we are or were at any
			// point in our history, in then our depth is the deepest depth of any of our referenced
			// objects, plus one
			return (depth != null ? depth.intValue() + 1 : 0);
		} finally {
			visited.remove(objectId.getId());
		}
	}

	protected int calculateDepth(T object) throws DfException {
		return calculateDocumentDepth(object, null);
	}

	@Override
	protected final int calculateDependencyTier(IDfSession session, T object) throws Exception {
		int depth = calculateDepth(object);
		if (isDfReference(object)) {
			depth++;
		}
		return depth;
	}

	@Override
	protected final String calculateHistoryId(IDfSession session, T object) throws Exception {
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
		if (ctx.isSupported(CmfObject.Archetype.FOLDER)) {
			final int pathCount = sysObject.getFolderIdCount();
			for (int i = 0; i < pathCount; i++) {
				IDfId folderId = sysObject.getFolderId(i);
				IDfFolder parent = session.getFolderBySpecification(folderId.getId());
				req.add(this.factory.newExportDelegate(session, parent));
			}
		}

		// We export our filestore
		if (ctx.isSupported(CmfObject.Archetype.DATASTORE)) {
			String storeName = sysObject.getStorageType();
			if (StringUtils.isNotBlank(storeName)) {
				IDfStore store = DfcUtils.getStore(session, storeName);
				if (store != null) {
					req.add(this.factory.newExportDelegate(session, store));
				} else {
					this.log.warn("SysObject {} refers to missing store [{}]", marshaled.getLabel(), storeName);
				}
			}
		}

		IDfReference ref = getReferenceFor(sysObject);
		if (ref != null) {
			IDfId referrentId = ref.getReferenceById();
			if (Tools.equals(referrentId.getDocbaseId(), sysObject.getObjectId().getDocbaseId())) {
				// The object is from the same docbase, so we make sure it's listed as a requirement
				// to ensure it gets copied AFTER the referrent gets copied
				DctmExportDelegate<?> delegate = this.factory.newExportDelegate(session,
					session.getObject(referrentId));
				if (delegate == null) {
					throw new ExportException(String.format(
						"The %s [%s](%s) is a reference to an object with ID (%s), but that object is not supported to be exported",
						marshaled.getType(), marshaled.getLabel(), marshaled.getId(), referrentId.getId()));
				}
				req.add(delegate);
			}

			// For references, we stop here
			return req;
		}

		// Export the format
		if (ctx.isSupported(CmfObject.Archetype.FORMAT)) {
			IDfFormat format = sysObject.getFormat();
			if (format != null) {
				req.add(this.factory.newExportDelegate(session, format));
			}
		}

		// Export the owner
		if (ctx.isSupported(CmfObject.Archetype.USER)) {
			String owner = DctmMappingUtils.substituteMappableUsers(session, sysObject.getOwnerName());
			if (!DctmMappingUtils.isSubstitutionForMappableUser(owner) && !ctx.isSpecialUser(owner)) {
				IDfUser user = session.getUser(sysObject.getOwnerName());
				if (user != null) {
					req.add(this.factory.newExportDelegate(session, user));
				}
			}
		}

		// Export the group
		if (ctx.isSupported(CmfObject.Archetype.GROUP)) {
			IDfGroup group = session.getGroup(sysObject.getGroupName());
			if (group != null) {
				req.add(this.factory.newExportDelegate(session, group));
			}
		}

		// Export the ACL requirements
		if (ctx.isSupported(CmfObject.Archetype.ACL)) {
			req.add(this.factory.newExportDelegate(session, sysObject.getACL()));
		}
		return req;
	}

	protected IDfReference getReferenceFor(T object) throws DfException {
		if ((object == null) || !object.isReference()) { return null; }
		final IDfSession s = object.getSession();
		try (DfcQuery query = new DfcQuery(s,
			String.format("select r_object_id from dm_reference_s where r_mirror_object_id = %s",
				DfcUtils.quoteString(object.getObjectId().getId())))) {
			if (!query.hasNext()) { return null; }
			return IDfReference.class.cast(s.getObject(query.next().getId("r_object_id")));
		}
	}

	@Override
	protected String calculateName(IDfSession session, T sysObject) throws Exception {
		return sysObject.getObjectName();
	}

	@Override
	protected Collection<CmfObjectRef> calculateParentIds(IDfSession session, T sysObject) throws Exception {
		List<CmfObjectRef> ret = new ArrayList<>();
		for (IDfValue v : DfValueFactory.getAllValues(DctmAttributes.I_FOLDER_ID, sysObject)) {
			ret.add(new CmfObjectRef(CmfObject.Archetype.FOLDER, v.asId().getId()));
		}
		return ret;
	}

	@Override
	protected boolean calculateHistoryCurrent(IDfSession session, T sysObject) throws Exception {
		return sysObject.getHasFolder();
	}
}