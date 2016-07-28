package com.armedia.cmf.engine.alfresco.bulk.importer;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import com.armedia.cmf.engine.alfresco.bulk.common.AlfRoot;
import com.armedia.cmf.engine.alfresco.bulk.common.AlfSessionWrapper;
import com.armedia.cmf.engine.importer.ImportDelegate;
import com.armedia.cmf.storage.CmfAttribute;
import com.armedia.cmf.storage.CmfDataType;
import com.armedia.cmf.storage.CmfEncodeableName;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfProperty;
import com.armedia.cmf.storage.CmfValue;

public abstract class AlfImportDelegate extends
	ImportDelegate<File, AlfRoot, AlfSessionWrapper, CmfValue, AlfImportContext, AlfImportDelegateFactory, AlfImportEngine> {

	protected static final TimeZone TZUTC = TimeZone.getTimeZone(("UTC"));

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

	protected AlfImportDelegate(AlfImportDelegateFactory factory, CmfObject<CmfValue> storedObject) throws Exception {
		super(factory, File.class, storedObject);
	}
}