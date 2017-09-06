package com.armedia.caliente.engine.ucm.importer;

import com.armedia.caliente.engine.importer.ImportDelegateFactory;
import com.armedia.caliente.engine.ucm.UcmSession;
import com.armedia.caliente.engine.ucm.UcmSessionWrapper;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class UcmImportDelegateFactory
	extends ImportDelegateFactory<UcmSession, UcmSessionWrapper, CmfValue, UcmImportContext, UcmImportEngine> {

	UcmImportDelegateFactory(UcmImportEngine engine, UcmSession session, CfgTools configuration) {
		super(engine, configuration);
	}

	@Override
	protected UcmImportDelegate<?> newImportDelegate(CmfObject<CmfValue> storedObject) throws Exception {
		return null;
	}
}