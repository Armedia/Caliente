package com.armedia.caliente.engine.ucm.exporter;

import com.armedia.caliente.engine.exporter.ExportDelegateFactory;
import com.armedia.caliente.engine.ucm.IdcSession;
import com.armedia.caliente.engine.ucm.UcmSessionWrapper;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class UcmExportDelegateFactory
	extends ExportDelegateFactory<IdcSession, UcmSessionWrapper, CmfValue, UcmExportContext, UcmExportEngine> {

	UcmExportDelegateFactory(UcmExportEngine engine, CfgTools configuration) {
		super(engine, configuration);
	}

	@Override
	protected UcmExportDelegate<?> newExportDelegate(IdcSession session, CmfType type, String searchKey)
		throws Exception {
		return null;
	}
}