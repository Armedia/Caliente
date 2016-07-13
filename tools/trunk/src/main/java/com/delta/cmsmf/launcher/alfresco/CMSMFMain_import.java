package com.delta.cmsmf.launcher.alfresco;

import java.io.File;
import java.util.Map;

import com.armedia.cmf.engine.alfresco.bulk.importer.AlfImportEngine;
import com.armedia.cmf.engine.xml.common.XmlSessionFactory;
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
		settings.put(XmlSessionFactory.ROOT, this.targetDir.getAbsolutePath());
		settings.put(XmlSessionFactory.DB, Setting.DB_DIRECTORY.getString());
		settings.put(XmlSessionFactory.CONTENT, Setting.CONTENT_DIRECTORY.getString());
		super.customizeSettings(settings);
	}
}