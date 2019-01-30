package com.armedia.caliente.engine.sharepoint.exporter;

import com.armedia.caliente.engine.exporter.ExportDelegateFactory;
import com.armedia.caliente.engine.sharepoint.ShptSession;
import com.armedia.caliente.engine.sharepoint.ShptSessionWrapper;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

public class ShptExportDelegateFactory
	extends ExportDelegateFactory<ShptSession, ShptSessionWrapper, CmfValue, ShptExportContext, ShptExportEngine> {

	protected ShptExportDelegateFactory(ShptExportEngine engine, CfgTools configuration) {
		super(engine, configuration);
	}

	@Override
	protected ShptExportDelegate<?> newExportDelegate(ShptSession session, CmfObject.Archetype type, String searchKey)
		throws Exception {
		switch (type) {
			case USER:
				return new ShptUser(this, session, session.getUser(Tools.decodeInteger(searchKey)));
			case GROUP:
				return new ShptGroup(this, session, session.getGroup(Tools.decodeInteger(searchKey)));
			case FOLDER:
				return new ShptFolder(this, session, session.getFolder(searchKey));
			case DOCUMENT:
				return ShptFile.locateFile(this, session, searchKey);
			default:
				throw new Exception(String.format("Unsupported object type [%s]", type));
		}
	}
}