package com.armedia.cmf.engine.cmis.importer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;

import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.importer.ImportOutcome;
import com.armedia.cmf.engine.importer.ImportResult;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.cmf.storage.CmfValueDecoderException;

public class CmisFolderDelegate extends CmisFileableDelegate<Folder> {

	public CmisFolderDelegate(CmisImportDelegateFactory factory, CmfObject<CmfValue> storedObject) throws Exception {
		super(factory, Folder.class, storedObject);
	}

	@Override
	protected Folder createNew(CmisImportContext ctx, Folder parent, Map<String, ?> properties) throws ImportException {
		return parent.createFolder(properties);
	}

	@Override
	protected boolean isMultifilable(Folder existing) {
		return false;
	}

	@Override
	protected ImportOutcome importObject(CmfAttributeTranslator<CmfValue> translator, CmisImportContext ctx)
		throws ImportException, CmfStorageException, CmfValueDecoderException {

		Map<String, Object> props = super.prepareProperties(translator, ctx);
		props.remove(PropertyIds.PATH);
		props.remove(PropertyIds.PARENT_ID);

		List<Folder> parents = getParentFolders(ctx);
		List<ImportOutcome> outcomes = new ArrayList<ImportOutcome>(parents.size());
		final String name = null;
		for (Folder f : getParentFolders(ctx)) {
			String path = String.format("%s/%s", f.getPath(), name);
			Folder existing = null;
			try {
				FileableCmisObject obj = FileableCmisObject.class.cast(ctx.getSession().getObjectByPath(path));
				if (obj instanceof Folder) {
					existing = Folder.class.cast(obj);
				}
			} catch (CmisObjectNotFoundException e) {
				existing = null;
			}

			if (existing == null) {
				// If it doesn't exist, we'll create the new object...
				existing = createNew(ctx, f, props);
				outcomes.add(new ImportOutcome(ImportResult.CREATED, existing.getId(), calculateNewLabel(existing)));
				continue;
			}

			if (isSameObject(existing)) {
				outcomes.add(new ImportOutcome(ImportResult.DUPLICATE, existing.getId(), calculateNewLabel(existing)));
				continue;
			}

			// Not the same...we must update the properties and/or content
			updateExisting(ctx, existing, props);
			outcomes.add(new ImportOutcome(ImportResult.UPDATED, existing.getId(), calculateNewLabel(existing)));
		}

		// Now select which outcome to return:
		// * Only return DUPLICATE if ALL entries are DUPLICATE.
		// * Only return UPDATED if there are no CREATED entries
		// * Otherwise, return the first CREATED entry
		ImportOutcome ret = null;
		for (ImportOutcome o : outcomes) {
			switch (o.getResult()) {
				case CREATED:
					return o;

				case UPDATED:
					if ((ret == null) || (ret.getResult() == ImportResult.DUPLICATE)) {
						ret = o;
					}
					continue;

				case DUPLICATE:
					if (ret == null) {
						ret = o;
					}
					continue;

				default:
					break;
			}
		}
		return ret;
	}
}