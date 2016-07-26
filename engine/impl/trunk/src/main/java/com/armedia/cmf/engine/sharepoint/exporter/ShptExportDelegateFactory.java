package com.armedia.cmf.engine.sharepoint.exporter;

import com.armedia.cmf.engine.exporter.ExportDelegateFactory;
import com.armedia.cmf.engine.sharepoint.Setting;
import com.armedia.cmf.engine.sharepoint.ShptSession;
import com.armedia.cmf.engine.sharepoint.ShptSessionWrapper;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

public class ShptExportDelegateFactory
	extends ExportDelegateFactory<ShptSession, ShptSessionWrapper, CmfValue, ShptExportContext, ShptExportEngine> {

	protected ShptExportDelegateFactory(ShptExportEngine engine, CfgTools configuration) {
		super(engine, configuration);
	}

	String getRelativePath(String relativeUrl) {
		String exportPath = String.format("%s/", getConfiguration().getString(Setting.PATH));
		if (relativeUrl.startsWith(exportPath)) {
			// The -1 is to account for the leading slash we want to preserve
			relativeUrl = relativeUrl.substring(exportPath.length() - 1);
		}
		return relativeUrl;
	}

	@Override
	protected ShptExportDelegate<?> newExportDelegate(ShptSession session, CmfType type, String searchKey)
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