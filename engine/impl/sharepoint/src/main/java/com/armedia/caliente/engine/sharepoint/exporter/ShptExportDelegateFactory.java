package com.armedia.caliente.engine.sharepoint.exporter;

import com.armedia.caliente.engine.exporter.ExportDelegateFactory;
import com.armedia.caliente.engine.sharepoint.ShptSession;
import com.armedia.caliente.engine.sharepoint.ShptSessionWrapper;
import com.armedia.caliente.engine.sharepoint.ShptSetting;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

public class ShptExportDelegateFactory
	extends ExportDelegateFactory<ShptSession, ShptSessionWrapper, CmfValue, ShptExportContext, ShptExportEngine> {

	protected ShptExportDelegateFactory(ShptExportEngine engine, CfgTools configuration) {
		super(engine, configuration);
	}

	String getRelativePath(String relativeUrl) {
		String exportPath = String.format("%s/", getConfiguration().getString(ShptSetting.PATH));
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