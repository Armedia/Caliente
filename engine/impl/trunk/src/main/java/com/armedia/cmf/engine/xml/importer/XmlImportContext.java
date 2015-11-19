package com.armedia.cmf.engine.xml.importer;

import org.slf4j.Logger;

import com.armedia.cmf.engine.importer.ImportContext;
import com.armedia.cmf.engine.xml.common.XmlRoot;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfObjectStore;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfTypeMapper;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class XmlImportContext extends ImportContext<XmlRoot, CmfValue, XmlImportContextFactory> {

	public XmlImportContext(XmlImportContextFactory factory, CfgTools settings, String rootId, CmfType rootType,
		XmlRoot session, Logger output, CmfTypeMapper typeMapper, CmfAttributeTranslator<CmfValue> translator,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?> streamStore) {
		super(factory, settings, rootId, rootType, session, output, typeMapper, translator, objectStore, streamStore);
	}
}