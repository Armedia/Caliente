package com.delta.cmsmf.launcher.cmis;

import java.util.Map;

import com.armedia.cmf.engine.cmis.importer.CmisImportEngine;
import com.delta.cmsmf.launcher.AbstractCMSMFMain_import;

public class CMSMFMain_import extends AbstractCMSMFMain_import {

	public CMSMFMain_import() throws Throwable {
		super(CmisImportEngine.getImportEngine());
	}

	@Override
	protected void customizeSettings(Map<String, Object> settings) {
		super.customizeSettings(settings);
		// TODO: Add the user, url, password, etc...
	}
}