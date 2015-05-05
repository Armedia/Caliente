package com.armedia.cmf.engine.cmis.importer;

import org.apache.chemistry.opencmis.client.api.Session;

import com.armedia.cmf.engine.cmis.CmisSessionWrapper;
import com.armedia.cmf.engine.importer.ImportDelegateFactory;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.CfgTools;

public class CmisImportDelegateFactory extends
	ImportDelegateFactory<Session, CmisSessionWrapper, StoredValue, CmisImportContext, CmisImportEngine> {

	CmisImportDelegateFactory(CmisImportEngine engine, CfgTools configuration) {
		super(engine, configuration);
	}
}
