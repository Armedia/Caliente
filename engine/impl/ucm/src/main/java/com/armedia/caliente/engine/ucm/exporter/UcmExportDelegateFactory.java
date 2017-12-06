package com.armedia.caliente.engine.ucm.exporter;

import java.net.URI;

import com.armedia.caliente.engine.exporter.ExportDelegateFactory;
import com.armedia.caliente.engine.ucm.UcmSession;
import com.armedia.caliente.engine.ucm.UcmSessionWrapper;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class UcmExportDelegateFactory
	extends ExportDelegateFactory<UcmSession, UcmSessionWrapper, CmfValue, UcmExportContext, UcmExportEngine> {

	UcmExportDelegateFactory(UcmExportEngine engine, CfgTools configuration) {
		super(engine, configuration);
	}

	@Override
	protected UcmExportDelegate<?> newExportDelegate(UcmSession session, CmfType type, String searchKey)
		throws Exception {
		URI uri = URI.create(searchKey);
		switch (type) {
			case DOCUMENT:
				return new UcmFileExportDelegate(this, session, session.getFile(uri));
			case FOLDER:
				return new UcmFolderExportDelegate(this, session, session.getFolder(uri));
			default:
				return null;
		}
	}
}