package com.armedia.cmf.engine.cmis.importer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;

import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.importer.ImportOutcome;
import com.armedia.cmf.engine.importer.ImportResult;
import com.armedia.cmf.storage.CmfAttribute;
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
	protected Collection<ImportOutcome> importObject(CmfAttributeTranslator<CmfValue> translator, CmisImportContext ctx)
		throws ImportException, CmfStorageException, CmfValueDecoderException {

		Map<String, Object> props = super.prepareProperties(translator, ctx);
		props.remove(PropertyIds.PATH);
		props.remove(PropertyIds.PARENT_ID);

		List<Folder> parents = getParentFolders(ctx);
		List<ImportOutcome> outcomes = new ArrayList<ImportOutcome>(parents.size());
		CmfAttribute<CmfValue> nameAtt = this.cmfObject.getAttribute(PropertyIds.NAME);
		if ((nameAtt == null) || !nameAtt.hasValues()) { throw new ImportException(String.format(
			"No %s attribute found for %s [%s](%s)", PropertyIds.NAME, this.cmfObject.getType(),
			this.cmfObject.getLabel(), this.cmfObject.getId())); }
		final CmfValue nameValue = nameAtt.getValue();
		if (nameValue.isNull()) { throw new ImportException(String.format(
			"%s attribute has a null value for %s [%s](%s)", PropertyIds.NAME, this.cmfObject.getType(),
			this.cmfObject.getLabel(), this.cmfObject.getId())); }
		final String name = nameValue.asString();
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
		return outcomes;
	}
}