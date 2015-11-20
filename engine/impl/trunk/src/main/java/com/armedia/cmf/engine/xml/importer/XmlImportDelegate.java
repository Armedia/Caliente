package com.armedia.cmf.engine.xml.importer;

import java.io.File;

import com.armedia.cmf.engine.importer.ImportDelegate;
import com.armedia.cmf.engine.xml.common.XmlRoot;
import com.armedia.cmf.engine.xml.common.XmlSessionWrapper;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfValue;

public abstract class XmlImportDelegate
	extends
	ImportDelegate<File, XmlRoot, XmlSessionWrapper, CmfValue, XmlImportContext, XmlImportDelegateFactory, XmlImportEngine> {

	protected XmlImportDelegate(XmlImportDelegateFactory factory, CmfObject<CmfValue> storedObject) throws Exception {
		super(factory, File.class, storedObject);
	}
}