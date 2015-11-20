package com.armedia.cmf.engine.xml.importer;

import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfValue;

abstract class XmlSharedFileImportDelegate<T> extends XmlImportDelegate {

	private final Class<T> xmlClass;

	protected XmlSharedFileImportDelegate(XmlImportDelegateFactory factory, CmfObject<CmfValue> storedObject,
		Class<T> xmlClass) throws Exception {
		super(factory, storedObject);
		this.xmlClass = xmlClass;
	}

	protected final T getXmlObject() {
		return this.factory.getXmlObject(this.cmfObject.getType(), this.xmlClass);
	}
}