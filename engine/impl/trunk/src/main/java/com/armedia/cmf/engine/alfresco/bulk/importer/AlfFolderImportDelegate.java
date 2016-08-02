package com.armedia.cmf.engine.alfresco.bulk.importer;

import java.util.Collection;
import java.util.Collections;

import com.armedia.cmf.engine.converter.IntermediateProperty;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.importer.ImportOutcome;
import com.armedia.cmf.engine.importer.ImportResult;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfProperty;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.cmf.storage.CmfValueDecoderException;

public class AlfFolderImportDelegate extends AlfImportDelegate {

	public AlfFolderImportDelegate(AlfImportDelegateFactory factory, CmfObject<CmfValue> storedObject)
		throws Exception {
		super(factory, storedObject);
	}

	@Override
	protected Collection<ImportOutcome> importObject(CmfAttributeTranslator<CmfValue> translator, AlfImportContext ctx)
		throws ImportException, CmfStorageException, CmfValueDecoderException {
		String path = null;
		CmfProperty<CmfValue> pathProp = this.cmfObject.getProperty(IntermediateProperty.PARENT_TREE_IDS);
		if (pathProp == null) { throw new ImportException(String.format(
			"Failed to find the required property [%s] in %s [%s](%s)", IntermediateProperty.PARENT_TREE_IDS.encode(),
			this.cmfObject.getSubtype(), this.cmfObject.getLabel(), this.cmfObject.getId())); }

		path = String.format("%s/%s", pathProp.getValue().asString(), this.cmfObject.getId());
		ctx.printf("ABI-DIRECTORY: [%s]->[%s]", this.cmfObject.getLabel(), path);
		return Collections.singleton(new ImportOutcome(ImportResult.CREATED, this.cmfObject.getId(), path));
	}
}