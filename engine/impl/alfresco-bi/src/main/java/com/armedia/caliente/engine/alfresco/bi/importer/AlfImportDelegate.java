package com.armedia.caliente.engine.alfresco.bi.importer;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import com.armedia.caliente.engine.alfresco.bi.AlfRoot;
import com.armedia.caliente.engine.alfresco.bi.AlfSessionWrapper;
import com.armedia.caliente.engine.importer.ImportDelegate;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfEncodeableName;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfValue;

public abstract class AlfImportDelegate extends
	ImportDelegate<File, AlfRoot, AlfSessionWrapper, CmfValue, AlfImportContext, AlfImportDelegateFactory, AlfImportEngine> {

	protected static final TimeZone TZUTC = TimeZone.getTimeZone("UTC");

	protected final CmfValue getAttributeValue(CmfEncodeableName attribute) {
		return getAttributeValue(attribute.encode());
	}

	protected final CmfValue getAttributeValue(String attribute) {
		CmfAttribute<CmfValue> att = this.cmfObject.getAttribute(attribute);
		if (att == null) { return CmfValue.Type.OTHER.getNull(); }
		if (att.hasValues()) { return att.getValue(); }
		return att.getType().getNull();
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
		if (att == null) { return CmfValue.Type.OTHER.getNull(); }
		if (att.hasValues()) { return att.getValue(); }
		return att.getType().getNull();
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