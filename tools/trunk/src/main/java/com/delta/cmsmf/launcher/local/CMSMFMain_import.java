package com.delta.cmsmf.launcher.local;

import com.armedia.cmf.engine.local.importer.LocalImportEngine;
import com.delta.cmsmf.launcher.AbstractCMSMFMain_import;

public class CMSMFMain_import extends AbstractCMSMFMain_import {
	public CMSMFMain_import() throws Throwable {
		super(LocalImportEngine.getImportEngine());
	}
}