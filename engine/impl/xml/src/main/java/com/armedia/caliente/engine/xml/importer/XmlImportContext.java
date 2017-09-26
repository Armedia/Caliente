package com.armedia.caliente.engine.xml.importer;

import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.importer.ImportContext;
import com.armedia.caliente.engine.xml.common.XmlRoot;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfTransformer;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class XmlImportContext extends ImportContext<XmlRoot, CmfValue, XmlImportContextFactory> {

	public XmlImportContext(XmlImportContextFactory factory, CfgTools settings, String rootId, CmfType rootType,
		XmlRoot session, Logger output, WarningTracker warningTracker, CmfTransformer typeMapper,
		CmfAttributeTranslator<CmfValue> translator, CmfObjectStore<?, ?> objectStore,
		CmfContentStore<?, ?, ?> streamStore, int batchPosition) {
		super(factory, settings, rootId, rootType, session, output, warningTracker, typeMapper, translator, objectStore,
			streamStore, batchPosition);
	}
}