package com.armedia.caliente.cli.caliente.launcher.alfresco;

import java.io.File;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.cli.caliente.cfg.CLIParam;
import com.armedia.caliente.cli.caliente.cfg.Setting;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.launcher.AbstractCalienteMain_import;
import com.armedia.caliente.engine.alfresco.bi.AlfSessionFactory;
import com.armedia.caliente.engine.alfresco.bi.importer.AlfImportEngine;

public class Caliente_import extends AbstractCalienteMain_import {

	private final File targetDir;

	public Caliente_import() throws Throwable {
		super(AlfImportEngine.getImportEngine());
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
		settings.put(AlfSessionFactory.ROOT, this.targetDir.getAbsolutePath());
		settings.put(AlfSessionFactory.DB, Setting.DB_DIRECTORY.getString());
		settings.put(AlfSessionFactory.CONTENT, Setting.CONTENT_DIRECTORY.getString());
		settings.put(AlfSessionFactory.CONTENT_MODEL, StringUtils.join(CLIParam.content_model.getAllString(), ','));
		settings.put(AlfSessionFactory.USER_MAP, Setting.USER_MAP.getString());
		settings.put(AlfSessionFactory.GROUP_MAP, Setting.GROUP_MAP.getString());
		settings.put(AlfSessionFactory.ROLE_MAP, Setting.ROLE_MAP.getString());
		super.customizeSettings(settings);
	}
}