package com.armedia.caliente.cli.caliente.launcher.xml;

import java.io.File;
import java.util.Map;

import com.armedia.caliente.cli.caliente.cfg.CLIParam;
import com.armedia.caliente.cli.caliente.cfg.Setting;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.launcher.AbstractCalienteMain_import;
import com.armedia.caliente.engine.xml.common.XmlSetting;
import com.armedia.caliente.engine.xml.importer.XmlImportEngine;

public class Caliente_import extends AbstractCalienteMain_import {

	private final File targetDir;

	public Caliente_import() throws Throwable {
		super(XmlImportEngine.getImportEngine());
		String target = CLIParam.repository.getString();
		if (target == null) {
			target = ".";
		}
		this.targetDir = new File(target).getCanonicalFile();
		this.targetDir.mkdirs();
		if (!this.targetDir.exists()) { throw new CalienteException(
			String.format("The target directory [%s] does not exist, and could not be created", this.targetDir)); }
		if (!this.targetDir.isDirectory()) { throw new CalienteException(
			String.format("A non-directory already exists at the location [%s] - can't continue", this.targetDir)); }
	}

	@Override
	protected void customizeSettings(Map<String, Object> settings) throws CalienteException {
		settings.put(XmlSetting.ROOT.getLabel(), this.targetDir.getAbsolutePath());
		settings.put(XmlSetting.DB.getLabel(), Setting.DB_DIRECTORY.getString());
		settings.put(XmlSetting.CONTENT.getLabel(), Setting.CONTENT_DIRECTORY.getString());
		super.customizeSettings(settings);
	}
}