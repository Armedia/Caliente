package com.armedia.cmf.engine.alfresco.bulk.importer;

import java.util.Collection;
import java.util.Collections;

import com.armedia.cmf.engine.alfresco.bulk.importer.jaxb.AggregatorBase;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.importer.ImportOutcome;
import com.armedia.cmf.engine.importer.ImportResult;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.cmf.storage.CmfValueDecoderException;

abstract class AlfAggregatedImportDelegate<I, T extends AggregatorBase<I>> extends AlfImportDelegate {

	private final Class<T> xmlClass;

	protected AlfAggregatedImportDelegate(AlfImportDelegateFactory factory, CmfObject<CmfValue> storedObject,
		Class<T> xmlClass) throws Exception {
		super(factory, storedObject);
		this.xmlClass = xmlClass;
	}

	@Override
	protected final Collection<ImportOutcome> importObject(CmfAttributeTranslator<CmfValue> translator,
		AlfImportContext ctx) throws ImportException, CmfStorageException, CmfValueDecoderException {
		I item = createItem(translator, ctx);
		if (item == null) { return Collections.singleton(ImportOutcome.SKIPPED); }
		getXmlObject().add(item);
		return Collections.singleton(new ImportOutcome(ImportResult.CREATED, this.cmfObject.getId(), this.cmfObject
			.getLabel()));
	}

	protected abstract I createItem(CmfAttributeTranslator<CmfValue> translator, AlfImportContext ctx)
		throws ImportException, CmfStorageException, CmfValueDecoderException;

	private T getXmlObject() {
		return this.factory.getXmlObject(this.cmfObject.getType(), this.xmlClass);
	}
}