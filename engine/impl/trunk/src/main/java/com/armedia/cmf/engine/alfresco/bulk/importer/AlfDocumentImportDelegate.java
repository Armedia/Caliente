package com.armedia.cmf.engine.alfresco.bulk.importer;

import java.util.Collection;
import java.util.Collections;

import com.armedia.cmf.engine.converter.IntermediateAttribute;
import com.armedia.cmf.engine.converter.IntermediateProperty;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.importer.ImportOutcome;
import com.armedia.cmf.engine.importer.ImportResult;
import com.armedia.cmf.storage.CmfAttribute;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfProperty;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.cmf.storage.CmfValueDecoderException;

public class AlfDocumentImportDelegate extends AlfImportDelegate {

	private final boolean head;
	private final int major;
	private final int minor;

	public AlfDocumentImportDelegate(AlfImportDelegateFactory factory, CmfObject<CmfValue> storedObject)
		throws Exception {
		super(factory, storedObject);
		CmfAttribute<CmfValue> isLatestVersionAtt = storedObject.getAttribute(IntermediateAttribute.IS_LATEST_VERSION);
		this.head = ((isLatestVersionAtt != null) && isLatestVersionAtt.hasValues()
			&& isLatestVersionAtt.getValue().asBoolean());

		CmfAttribute<CmfValue> versionLabelAtt = storedObject.getAttribute(IntermediateAttribute.VERSION_LABEL);
		// Try to parse out the major version, and get the counter from the factory for how many
		// items for that major version have been processed so far
		this.major = 0;
		this.minor = 0;
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
		ctx.printf("ABI-DOCUMENT: [%s]->[%s]", this.cmfObject.getLabel(), path);
		return Collections.singleton(new ImportOutcome(ImportResult.CREATED, this.cmfObject.getId(), path));
	}
}