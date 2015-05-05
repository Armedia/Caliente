package com.armedia.cmf.engine.sharepoint.importer;

import com.armedia.cmf.engine.importer.ImportDelegateFactory;
import com.armedia.cmf.engine.sharepoint.ShptSession;
import com.armedia.cmf.engine.sharepoint.ShptSessionWrapper;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.CfgTools;

public class ShptImportDelegateFactory extends
	ImportDelegateFactory<ShptSession, ShptSessionWrapper, StoredValue, ShptImportContext, ShptImportEngine> {

	protected ShptImportDelegateFactory(ShptImportEngine engine, CfgTools configuration) {
		super(engine, configuration);
	}
}