package com.armedia.caliente.engine.ucm.importer;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.Folder;

import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.engine.importer.ImportOutcome;
import com.armedia.caliente.engine.ucm.model.UcmFSObject;
import com.armedia.caliente.engine.ucm.model.UcmFolder;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfValue;

public abstract class UcmFSObjectDelegate<T extends UcmFSObject> extends UcmImportDelegate<T> {

	public UcmFSObjectDelegate(UcmImportDelegateFactory factory, Class<T> klass, CmfObject<CmfValue> storedObject)
		throws Exception {
		super(factory, klass, storedObject);
	}

	protected abstract T createNew(UcmImportContext ctx, UcmFolder parent, Map<String, Object> properties)
		throws ImportException;

	protected void updateExisting(UcmImportContext ctx, T existing, Map<String, Object> properties)
		throws ImportException {

	}

	protected T createNewVersion(UcmImportContext ctx, T existing, Map<String, Object> properties)
		throws ImportException {
		return existing;
	}

	protected boolean isVersionable(T existing) {
		return false;
	}

	protected boolean isSameObject(T existing) {
		// Calendar creationDate = existing.getCreationDate();
		// Calendar modificationDate = existing.getLastModificationDate();
		return true;
	}

	protected abstract boolean isMultifilable(T existing);

	protected String calculateNewLabel(T existing) {
		/*
		List<String> paths = existing.getPaths();
		String versionLabel = "";
		if (isVersionable(existing)) {
			versionLabel = String.format("#%s", existing.getProperty(PropertyIds.VERSION_LABEL).getValueAsString());
		}
		if ((paths == null)
			|| paths.isEmpty()) { return String.format("<unfiled>::%s%s", existing.getName(), versionLabel); }
		String path = paths.get(0);
		return String.format("%s%s", path, versionLabel);
		*/
		return null;
	}

	protected void linkToParents(UcmImportContext ctx, T existing, List<Folder> finalParents) {
	}

	/*
	private void setMapping(UcmImportContext ctx, T existing) {
		ctx.getAttributeMapper().setMapping(this.cmfObject.getType(), PropertyIds.OBJECT_ID, this.cmfObject.getId(),
			existing.getId());
	}
		*/

	@Override
	protected Collection<ImportOutcome> importObject(CmfAttributeTranslator<CmfValue> translator, UcmImportContext ctx)
		throws ImportException, CmfStorageException {
		return null;
	}
}