package com.delta.cmsmf.launcher.alfresco;

import java.io.File;
import java.util.Map;

import com.armedia.cmf.engine.alfresco.bulk.common.AlfSessionFactory;
import com.armedia.cmf.engine.alfresco.bulk.importer.AlfImportEngine;
import com.delta.cmsmf.cfg.CLIParam;
import com.delta.cmsmf.cfg.Setting;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.launcher.AbstractCMSMFMain_import;

public class CMSMFMain_import extends AbstractCMSMFMain_import {

	private final File targetDir;

	public CMSMFMain_import() throws Throwable {
		super(AlfImportEngine.getImportEngine());
		String target = CLIParam.repository.getString();
		if (target == null) {
			target = ".";
		}
		this.targetDir = new File(target).getCanonicalFile();
		this.targetDir.mkdirs();
		if (!this.targetDir.exists()) { throw new CMSMFException(
			String.format("The target directory [%s] does not exist, and could not be created", this.targetDir)); }
		if (!this.targetDir.isDirectory()) { throw new CMSMFException(
			String.format("A non-directory already exists at the location [%s] - can't continue", this.targetDir)); }
	}

	@Override
	protected void customizeSettings(Map<String, Object> settings) throws CMSMFException {
		settings.put(AlfSessionFactory.ROOT, this.targetDir.getAbsolutePath());
		settings.put(AlfSessionFactory.DB, Setting.DB_DIRECTORY.getString());
		settings.put(AlfSessionFactory.CONTENT, Setting.CONTENT_DIRECTORY.getString());
		settings.put(AlfSessionFactory.CONTENT_MODEL, CLIParam.content_model.getString());
		settings.put(AlfSessionFactory.USER_MAP, Setting.USER_MAP.getString());
		settings.put(AlfSessionFactory.GROUP_MAP, Setting.GROUP_MAP.getString());
		settings.put(AlfSessionFactory.ROLE_MAP, Setting.ROLE_MAP.getString());
		settings.put(AlfSessionFactory.TYPE_MAP, Setting.TYPE_MAP.getString());
		super.customizeSettings(settings);
	}
}