package com.armedia.cmf.engine.cmis.importer;

import org.apache.chemistry.opencmis.client.api.Session;

import com.armedia.cmf.engine.cmis.CmisSessionWrapper;
import com.armedia.cmf.engine.importer.ImportDelegateFactory;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class CmisImportDelegateFactory extends
	ImportDelegateFactory<Session, CmisSessionWrapper, CmfValue, CmisImportContext, CmisImportEngine> {

	CmisImportDelegateFactory(CmisImportEngine engine, CfgTools configuration) {
		super(engine, configuration);
	}

	@Override
	protected CmisImportDelegate<?> newImportDelegate(CmfObject<CmfValue> storedObject) throws Exception {
		return null;
	}
}