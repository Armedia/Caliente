package com.armedia.cmf.engine.xml.importer;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.importer.ImportOutcome;
import com.armedia.cmf.engine.importer.ImportResult;
import com.armedia.cmf.engine.xml.importer.jaxb.AggregatorBase;
import com.armedia.cmf.engine.xml.importer.jaxb.AttributeT;
import com.armedia.cmf.engine.xml.importer.jaxb.DataTypeT;
import com.armedia.cmf.storage.CmfAttribute;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.cmf.storage.CmfValueDecoderException;

abstract class XmlAggregatedImportDelegate<I, T extends AggregatorBase<I>> extends XmlImportDelegate {

	private final Class<T> xmlClass;

	protected XmlAggregatedImportDelegate(XmlImportDelegateFactory factory, CmfObject<CmfValue> storedObject,
		Class<T> xmlClass) throws Exception {
		super(factory, storedObject);
		this.xmlClass = xmlClass;
	}

	protected final void dumpAttributes(List<AttributeT> list) {
		for (String name : this.cmfObject.getAttributeNames()) {
			final AttributeT attribute = new AttributeT();
			final CmfAttribute<CmfValue> att = this.cmfObject.getAttribute(name);

			attribute.setName(name);
			attribute.setDataType(DataTypeT.convert(att.getType()));
			for (CmfValue v : att) {
				attribute.getValue().add(v.asString());
			}

			list.add(attribute);
		}
	}

	@Override
	protected final Collection<ImportOutcome> importObject(CmfAttributeTranslator<CmfValue> translator,
		XmlImportContext ctx) throws ImportException, CmfStorageException, CmfValueDecoderException {
		I item = createItem(translator, ctx);
		if (item == null) { return Collections.singleton(ImportOutcome.SKIPPED); }
		getXmlObject().add(item);
		return Collections.singleton(new ImportOutcome(ImportResult.CREATED, this.cmfObject.getId(), this.cmfObject
			.getLabel()));
	}

	protected abstract I createItem(CmfAttributeTranslator<CmfValue> translator, XmlImportContext ctx)
		throws ImportException, CmfStorageException, CmfValueDecoderException;

	private T getXmlObject() {
		return this.factory.getXmlObject(this.cmfObject.getType(), this.xmlClass);
	}
}