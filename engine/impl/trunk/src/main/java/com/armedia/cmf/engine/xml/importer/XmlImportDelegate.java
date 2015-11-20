package com.armedia.cmf.engine.xml.importer;

import java.io.File;
import java.util.Collection;

import com.armedia.cmf.engine.importer.ImportDelegate;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.importer.ImportOutcome;
import com.armedia.cmf.engine.xml.common.XmlRoot;
import com.armedia.cmf.engine.xml.common.XmlSessionWrapper;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.cmf.storage.CmfValueDecoderException;

public abstract class XmlImportDelegate
extends
ImportDelegate<File, XmlRoot, XmlSessionWrapper, CmfValue, XmlImportContext, XmlImportDelegateFactory, XmlImportEngine> {

	protected XmlImportDelegate(XmlImportDelegateFactory factory, CmfObject<CmfValue> storedObject) throws Exception {
		super(factory, File.class, storedObject);
	}
}