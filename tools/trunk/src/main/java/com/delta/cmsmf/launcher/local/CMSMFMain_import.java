package com.delta.cmsmf.launcher.local;

import java.io.File;
import java.util.Map;

import com.armedia.cmf.engine.local.common.LocalSessionFactory;
import com.armedia.cmf.engine.local.importer.LocalImportEngine;
import com.delta.cmsmf.cfg.CLIParam;
import com.delta.cmsmf.cfg.Setting;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.launcher.AbstractCMSMFMain_import;

public class CMSMFMain_import extends AbstractCMSMFMain_import {

	private final File targetDir;

	public CMSMFMain_import() throws Throwable {
		super(LocalImportEngine.getImportEngine());
		String target = Setting.CMF_IMPORT_TARGET_LOCATION.getString();
		if (target == null) { throw new CMSMFException(
			String
				.format(
					"Missing target location - must provide a directory to import into (using the %s parameter or the %s configuration setting)",
					CLIParam.cmf_import_target_location.option.getLongOpt(), Setting.CMF_IMPORT_TARGET_LOCATION.name)); }
		this.targetDir = new File(target).getCanonicalFile();
		this.targetDir.mkdirs();
		if (!this.targetDir.exists()) { throw new CMSMFException(String.format(
			"The target directory [%s] does not exist, and could not be created", this.targetDir)); }
		if (!this.targetDir.isDirectory()) { throw new CMSMFException(String.format(
			"A non-directory already exists at the location [%s] - can't continue", this.targetDir)); }
	}

	@Override
	protected void customizeSettings(Map<String, Object> settings) throws CMSMFException {
		settings.put(LocalSessionFactory.ROOT, this.targetDir.getAbsolutePath());
		super.customizeSettings(settings);
	}
}