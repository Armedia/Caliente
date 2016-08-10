package com.armedia.cmf.engine.alfresco.bulk.indexer;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import com.armedia.cmf.engine.converter.IntermediateProperty;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.importer.ImportOutcome;
import com.armedia.cmf.engine.importer.ImportResult;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfProperty;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfValue;

public class AlfFolderIndexerDelegate extends AlfIndexerDelegate {

	public AlfFolderIndexerDelegate(AlfIndexerDelegateFactory factory, CmfObject<CmfValue> storedObject)
		throws Exception {
		super(factory, storedObject);
	}

	@Override
	protected Collection<ImportOutcome> importObject(CmfAttributeTranslator<CmfValue> translator, AlfIndexerContext ctx)
		throws ImportException, CmfStorageException {
		String path = null;
		CmfProperty<CmfValue> pathProp = this.cmfObject.getProperty(IntermediateProperty.PARENT_TREE_IDS);
		if (pathProp == null) { throw new ImportException(String.format(
			"Failed to find the required property [%s] in %s [%s](%s)", IntermediateProperty.PARENT_TREE_IDS.encode(),
			this.cmfObject.getSubtype(), this.cmfObject.getLabel(), this.cmfObject.getId())); }

		String prefix = (pathProp.hasValues() ? pathProp.getValue().asString() : "");
		path = String.format("%s%s%s", prefix, StringUtils.isEmpty(prefix) ? "" : "/", this.cmfObject.getId());

		File directory = this.factory.getLocation(path);
		File metadata = this.factory.getLocation(String.format("%s-metadata.xml", path));

		StringBuilder sb = new StringBuilder();
		sb.append(String.format("ABI-DIRECTORY: [%s]->[%s || %s]%n", this.cmfObject.getLabel(), directory, metadata));

		Set<String> attNames = new TreeSet<String>(this.cmfObject.getAttributeNames());
		sb.append(String.format("\tATTNAMES for [%s] = %s%n", this.cmfObject.getSubtype(), attNames));
		Set<String> propNames = new TreeSet<String>(this.cmfObject.getPropertyNames());
		sb.append(String.format("\tPROPNAMES for [%s] = %s%n", this.cmfObject.getSubtype(), propNames));

		ctx.printf(sb.toString());
		return Collections.singleton(new ImportOutcome(ImportResult.CREATED, this.cmfObject.getId(), path));
	}
}