package com.armedia.caliente.engine.xml.importer;

import java.util.Collection;
import java.util.Collections;

import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.engine.importer.ImportOutcome;
import com.armedia.caliente.engine.importer.ImportResult;
import com.armedia.caliente.engine.importer.TypeDescriptor;
import com.armedia.caliente.engine.xml.importer.jaxb.AggregatorBase;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfValue;

abstract class XmlAggregatedImportDelegate<I, T extends AggregatorBase<I>> extends XmlImportDelegate {

	private final Class<T> xmlClass;

	protected XmlAggregatedImportDelegate(XmlImportDelegateFactory factory, CmfObject<CmfValue> storedObject,
		Class<T> xmlClass) throws Exception {
		super(factory, storedObject);
		this.xmlClass = xmlClass;
	}

	@Override
	protected final Collection<ImportOutcome> importObject(TypeDescriptor targetType,
		CmfAttributeTranslator<CmfValue> translator, XmlImportContext ctx) throws ImportException, CmfStorageException {
		I item = createItem(translator, ctx);
		if (item == null) { return Collections.singleton(ImportOutcome.SKIPPED); }
		getXmlObject().add(item);
		return Collections
			.singleton(new ImportOutcome(ImportResult.CREATED, this.cmfObject.getId(), this.cmfObject.getLabel()));
	}

	protected abstract I createItem(CmfAttributeTranslator<CmfValue> translator, XmlImportContext ctx)
		throws ImportException, CmfStorageException;

	private T getXmlObject() {
		return this.factory.getXmlObject(this.cmfObject.getType(), this.xmlClass);
	}
}