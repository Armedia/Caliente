package com.armedia.caliente.cli.caliente.launcher.local;

import java.io.File;
import java.util.Map;

import com.armedia.caliente.cli.caliente.cfg.CLIParam;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.launcher.AbstractCalienteMain_import;
import com.armedia.caliente.engine.local.common.LocalSetting;
import com.armedia.caliente.engine.local.importer.LocalImportEngine;

public class Caliente_import extends AbstractCalienteMain_import {

	private final File targetDir;

	public Caliente_import() throws Throwable {
		super(LocalImportEngine.getImportEngine());
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
		settings.put(LocalSetting.ROOT.getLabel(), this.targetDir.getAbsolutePath());
		// TODO: Enable these, but get them from a system-wide configuration
		/*
		settings.put(Setting.COPY_CONTENT.getLabel(), false);
		settings.put(Setting.INCLUDE_ALL_VERSIONS.getLabel(), false);
		settings.put(Setting.FAIL_ON_COLLISIONS.getLabel(), false);
		 */
		super.customizeSettings(settings);
	}
}