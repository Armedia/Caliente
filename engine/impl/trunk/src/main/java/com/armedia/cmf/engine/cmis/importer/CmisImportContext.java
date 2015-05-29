package com.armedia.cmf.engine.cmis.importer;

import org.apache.chemistry.opencmis.client.api.Session;
import org.slf4j.Logger;

import com.armedia.cmf.engine.importer.ImportContext;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfObjectStore;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;

public class CmisImportContext extends ImportContext<Session, CmfValue, CmisImportContextFactory> {

	CmisImportContext(CmisImportContextFactory factory, String rootId, CmfType rootType, Session session,
		Logger output, CmfAttributeTranslator<CmfValue> translator, CmfObjectStore<?, ?> objectStore,
		CmfContentStore<?> streamStore) {
		super(factory, factory.getSettings(), rootId, rootType, session, output, translator, objectStore, streamStore);
	}
}