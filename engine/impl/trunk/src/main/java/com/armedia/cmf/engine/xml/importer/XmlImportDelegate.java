package com.armedia.cmf.engine.xml.importer;

import java.io.File;
import java.util.Collections;
import java.util.List;

import com.armedia.cmf.engine.importer.ImportDelegate;
import com.armedia.cmf.engine.xml.common.XmlRoot;
import com.armedia.cmf.engine.xml.common.XmlSessionWrapper;
import com.armedia.cmf.engine.xml.importer.jaxb.AttributeT;
import com.armedia.cmf.engine.xml.importer.jaxb.DataTypeT;
import com.armedia.cmf.storage.CmfAttribute;
import com.armedia.cmf.storage.CmfDataType;
import com.armedia.cmf.storage.CmfEncodeableName;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfProperty;
import com.armedia.cmf.storage.CmfValue;

public abstract class XmlImportDelegate
	extends
	ImportDelegate<File, XmlRoot, XmlSessionWrapper, CmfValue, XmlImportContext, XmlImportDelegateFactory, XmlImportEngine> {

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

	protected final CmfValue getAttributeValue(CmfEncodeableName attribute) {
		return getAttributeValue(attribute.encode());
	}

	protected final CmfValue getAttributeValue(String attribute) {
		CmfAttribute<CmfValue> att = this.cmfObject.getAttribute(attribute);
		if (att == null) { return CmfValue.NULL.get(CmfDataType.OTHER); }
		if (att.hasValues()) { return att.getValue(); }
		return CmfValue.NULL.get(att.getType());
	}

	protected final List<CmfValue> getAttributeValues(CmfEncodeableName attribute) {
		return getAttributeValues(attribute.encode());
	}

	protected final List<CmfValue> getAttributeValues(String attribute) {
		CmfAttribute<CmfValue> att = this.cmfObject.getAttribute(attribute);
		if (att == null) { return Collections.emptyList(); }
		return att.getValues();
	}

	protected final CmfValue getPropertyValue(CmfEncodeableName attribute) {
		return getPropertyValue(attribute.encode());
	}

	protected final CmfValue getPropertyValue(String attribute) {
		CmfProperty<CmfValue> att = this.cmfObject.getProperty(attribute);
		if (att == null) { return CmfValue.NULL.get(CmfDataType.OTHER); }
		if (att.hasValues()) { return att.getValue(); }
		return CmfValue.NULL.get(att.getType());
	}

	protected final List<CmfValue> getPropertyValues(CmfEncodeableName attribute) {
		return getPropertyValues(attribute.encode());
	}

	protected final List<CmfValue> getPropertyValues(String attribute) {
		CmfProperty<CmfValue> att = this.cmfObject.getProperty(attribute);
		if (att == null) { return Collections.emptyList(); }
		return att.getValues();
	}

	protected XmlImportDelegate(XmlImportDelegateFactory factory, CmfObject<CmfValue> storedObject) throws Exception {
		super(factory, File.class, storedObject);
	}
}