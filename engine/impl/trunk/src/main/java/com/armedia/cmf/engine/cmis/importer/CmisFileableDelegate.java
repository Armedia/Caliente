package com.armedia.cmf.engine.cmis.importer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.runtime.ObjectIdImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;

import com.armedia.cmf.engine.converter.IntermediateProperty;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.importer.ImportOutcome;
import com.armedia.cmf.engine.importer.ImportResult;
import com.armedia.cmf.storage.CmfAttribute;
import com.armedia.cmf.storage.CmfAttributeMapper.Mapping;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfProperty;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.cmf.storage.CmfValueDecoderException;

public abstract class CmisFileableDelegate<T extends FileableCmisObject> extends CmisImportDelegate<T> {

	public CmisFileableDelegate(CmisImportDelegateFactory factory, Class<T> klass, CmfObject<CmfValue> storedObject)
		throws Exception {
		super(factory, klass, storedObject);
	}

	protected List<Folder> getParentFolders(CmisImportContext ctx) throws ImportException {

		final List<Folder> ret = new ArrayList<Folder>();
		final Session session = ctx.getSession();

		// Try by IDs
		CmfProperty<CmfValue> prop = this.cmfObject.getProperty(IntermediateProperty.PARENT_ID);
		if ((prop != null) && prop.hasValues()) {
			for (CmfValue v : prop) {
				if (v.isNull()) {
					continue;
				}
				Mapping m = ctx.getAttributeMapper().getTargetMapping(CmfType.FOLDER, PropertyIds.OBJECT_ID, v.asId());
				if (m == null) {
					// no mapping, we simply skip...right?
					continue;
				}
				try {
					CmisObject obj = session.getObject(m.getTargetValue());
					if ((obj != null) && (obj instanceof Folder)) {
						ret.add(Folder.class.cast(obj));
					}
				} catch (CmisObjectNotFoundException e) {
					// Ignore a missing parent
				}
			}
		} else {
			// Parents couldn't be found by ID... so resort to finding
			// them by path
			prop = this.cmfObject.getProperty(IntermediateProperty.PATH);
			if ((prop != null) && prop.hasValues()) {
				for (CmfValue v : prop) {
					if (v.isNull()) {
						continue;
					}
					String path = ctx.getTargetPath(v.asString());
					try {
						CmisObject obj = session.getObjectByPath(path);
						if ((obj != null) && (obj instanceof Folder)) {
							ret.add(Folder.class.cast(obj));
						}
					} catch (CmisObjectNotFoundException e) {
						// Ignore a missing parent
					}
				}
			}
		}

		if (ret.isEmpty()) {
			if (ctx.isPathAltering()) {
				// If there are no parents, but the path needs to be altered, then we proceed
				// to locate the actual target path based on the "new root"
				String path = ctx.getTargetPath("/");
				try {
					CmisObject obj = session.getObjectByPath(path);
					if ((obj != null) && (obj instanceof Folder)) {
						ret.add(Folder.class.cast(obj));
					}
				} catch (CmisObjectNotFoundException e) {
					// Ignore a missing parent
				}
			} else {
				// If there are no parents, then the root folder is the parent
				ret.add(session.getRootFolder());
			}
		}
		return ret;
	}

	protected T findExisting(CmisImportContext ctx, List<Folder> parents) throws ImportException {
		final Session session = ctx.getSession();
		CmfAttribute<CmfValue> att = this.cmfObject.getAttribute(PropertyIds.NAME);
		if ((att == null) || !att.hasValues()) { return null; }
		CmfValue v = att.getValue();
		if (v.isNull()) { return null; }
		final String name = v.asString();
		for (Folder p : parents) {
			try {
				CmisObject obj = session.getObjectByPath(String.format("%s/%s", p.getPath(), name));
				if (this.objectClass.isInstance(obj)) { return this.objectClass.cast(obj); }
			} catch (CmisObjectNotFoundException e) {
				// Do nothing...
				continue;
			}
		}
		return null;
	}

	protected abstract T createNew(CmisImportContext ctx, Folder parent, Map<String, ?> properties)
		throws ImportException;

	protected void updateExisting(CmisImportContext ctx, T existing, Map<String, ?> properties) throws ImportException {
		existing.updateProperties(properties);
	}

	protected T createNewVersion(CmisImportContext ctx, T existing, Map<String, ?> properties) throws ImportException {
		return existing;
	}

	protected boolean isVersionable(T existing) {
		return false;
	}

	protected boolean isSameObject(T existing) {
		return false;
	}

	protected abstract boolean isMultifilable(T existing);

	protected String calculateNewLabel(T existing) {
		List<String> paths = existing.getPaths();
		String versionLabel = "";
		if (isVersionable(existing)) {
			versionLabel = String.format("#%s", existing.getProperty(PropertyIds.VERSION_LABEL).getValueAsString());
		}
		if ((paths == null) || paths.isEmpty()) { return String.format("<unfiled>::%s%s", existing.getName(),
			versionLabel); }
		String path = paths.get(0);
		return String.format("%s%s", path, versionLabel);
	}

	protected void linkToParents(CmisImportContext ctx, T existing, List<Folder> finalParents) {
		final ObjectId id = new ObjectIdImpl(existing.getId());
		Map<String, Folder> oldParents = new HashMap<String, Folder>();
		Map<String, Folder> newParents = new HashMap<String, Folder>();
		for (Folder f : finalParents) {
			newParents.put(f.getId(), f);
		}
		for (Folder f : existing.getParents()) {
			oldParents.put(f.getId(), f);
		}
		Set<String> oldParentsIds = new HashSet<String>(oldParents.keySet());
		Set<String> newParentsIds = new HashSet<String>(newParents.keySet());
		Set<String> bothParentsIds = new HashSet<String>();

		// Remove those that need not be unlinked
		for (String s : newParentsIds) {
			oldParents.remove(s);
		}

		// Remove those that need not be re-linked
		for (String s : oldParentsIds) {
			newParents.remove(s);
		}
		bothParentsIds.addAll(oldParentsIds);
		bothParentsIds.retainAll(newParentsIds);

		final boolean multifile = ctx.getRepositoryInfo().getCapabilities().isMultifilingSupported()
			&& isMultifilable(existing);

		// Unlink from those no longer needed
		for (String s : oldParents.keySet()) {
			oldParents.get(s).removeFromFolder(id);
		}

		if (multifile || bothParentsIds.isEmpty()) {
			// Link to those needed but not yet linked
			for (String s : newParents.keySet()) {
				newParents.get(s).addToFolder(id, false);
				if (!multifile) {
					break;
				}
			}
		}

		if (!newParents.isEmpty() || !oldParents.isEmpty()) {
			existing.refresh();
		}
	}

	private void setMapping(CmisImportContext ctx, T existing) {
		ctx.getAttributeMapper().setMapping(this.cmfObject.getType(), PropertyIds.OBJECT_ID, this.cmfObject.getId(),
			existing.getId());
	}

	@Override
	protected final ImportOutcome importObject(CmfAttributeTranslator<CmfValue> translator, CmisImportContext ctx)
		throws ImportException, CmfStorageException, CmfValueDecoderException {

		Map<String, Object> props = super.prepareProperties(translator, ctx);
		props.remove(PropertyIds.PATH);
		props.remove(PropertyIds.PARENT_ID);

		List<Folder> parents = getParentFolders(ctx);
		// Find the parent folder...
		final Folder parent = parents.get(0);

		// First, try to find the existing object.
		T existing = findExisting(ctx, parents);
		if (existing == null) {
			// If it doesn't exist, we'll create the new object...
			existing = createNew(ctx, parent, props);
			linkToParents(ctx, existing, parents);
			setMapping(ctx, existing);
			return new ImportOutcome(ImportResult.CREATED, existing.getId(), calculateNewLabel(existing));
		}

		if (isSameObject(existing)) { return new ImportOutcome(ImportResult.DUPLICATE); }
		if (isVersionable(existing)) {
			existing = createNewVersion(ctx, existing, props);
			linkToParents(ctx, existing, parents);
			setMapping(ctx, existing);
			return new ImportOutcome(ImportResult.CREATED, existing.getId(), calculateNewLabel(existing));
		} else {
			// Not the same...we must update the properties and/or content
			updateExisting(ctx, existing, props);
		}
		linkToParents(ctx, existing, parents);
		setMapping(ctx, existing);
		return new ImportOutcome(ImportResult.UPDATED, existing.getId(), calculateNewLabel(existing));
	}
}