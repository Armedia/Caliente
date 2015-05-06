package com.armedia.cmf.engine.sharepoint.exporter;

import com.armedia.cmf.engine.exporter.ExportDelegateFactory;
import com.armedia.cmf.engine.sharepoint.ShptSession;
import com.armedia.cmf.engine.sharepoint.ShptSessionWrapper;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

public class ShptExportDelegateFactory extends
ExportDelegateFactory<ShptSession, ShptSessionWrapper, StoredValue, ShptExportContext, ShptExportEngine> {

	protected ShptExportDelegateFactory(ShptExportEngine engine, CfgTools configuration) {
		super(engine, configuration);
	}

	@Override
	protected ShptExportDelegate<?> newExportDelegate(ShptSession session, StoredObjectType type, String searchKey)
		throws Exception {
		switch (type) {
			case USER:
				return new ShptUser(this, session.getUser(Tools.decodeInteger(searchKey)));
			case GROUP:
				return new ShptGroup(this, session.getGroup(Tools.decodeInteger(searchKey)));
			case FOLDER:
				return new ShptFolder(this, session.getFolder(searchKey));
			case DOCUMENT:
				return ShptFile.locateFile(this, session, searchKey);
			default:
				throw new Exception(String.format("Unsupported object type [%s]", type));
		}
	}
}