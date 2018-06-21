package com.armedia.caliente.cli.caliente.newlauncher.local;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.caliente.cfg.CLIParam;
import com.armedia.caliente.cli.caliente.command.ExportCommandModule;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.local.common.LocalSetting;
import com.armedia.commons.utilities.Tools;

class LocalExporter extends ExportCommandModule {
	LocalExporter(ExportEngine<?, ?, ?, ?, ?, ?> engine) {
		super(engine);
	}

	@Override
	protected boolean preInitialize(Map<String, Object> settings) {
		return super.preInitialize(settings);
	}

	@Override
	protected boolean doInitialize(Map<String, Object> settings) {
		return super.doInitialize(settings);
	}

	@Override
	protected boolean postInitialize(Map<String, Object> settings) {
		return super.postInitialize(settings);
	}

	@Override
	protected void preValidateSettings(Map<String, Object> settings) throws CalienteException {
		super.preValidateSettings(settings);
	}

	@Override
	protected boolean preConfigure(OptionValues commandValues, Map<String, Object> settings)
		throws CalienteException {
		return super.preConfigure(commandValues, settings);
	}

	protected boolean isCopyContent(OptionValues commandValues) {
		return commandValues.isPresent(CLIParam.copy_content) && !commandValues.isPresent(CLIParam.skip_content);
	}

	/*
	protected void customizeContentStoreProperties(StoreConfiguration cfg) {
		super.customizeContentStoreProperties(cfg);
		cfg.getSettings().put(LocalContentStoreSetting.IGNORE_DESCRIPTOR.getLabel(), Boolean.TRUE.toString());
	}
	
	protected File getContentFilesLocation() {
		if (isCopyContent()) { return super.getContentFilesLocation(); }
		return new File(BaseParam.source.getString());
	}
	
	protected String getContentStrategyName() {
		return LocalOrganizationStrategy.NAME;
	}
	*/

	@Override
	protected boolean doConfigure(OptionValues commandValues, Map<String, Object> settings)
		throws CalienteException {
		if (!super.doConfigure(commandValues, settings)) { return false; }

		File source = Tools.canonicalize(new File(commandValues.getString(CLIParam.source)));

		// Make sure a source has been specified
		if (source == null) { throw new CalienteException("Must specify a source to export from"); }
		if (!source.exists()) { throw new CalienteException(
			String.format("The specified source at [%s] does not exist", source.getPath())); }
		if (!source.exists()) { throw new CalienteException(
			String.format("The specified source at [%s] does not exist", source.getPath())); }
		if (!source.isDirectory()) { throw new CalienteException(
			String.format("The specified source at [%s] is not a directory", source.getPath())); }
		if (!source.canRead()) { throw new CalienteException(
			String.format("The specified source at [%s] is not readable", source.getPath())); }
		try {
			File f = source.getCanonicalFile();
			source = f;
		} catch (IOException e) {
			File f = source.getAbsoluteFile();
			if (this.log.isTraceEnabled()) {
				this.log.warn(String.format(
					"Failed to find the canonical path for [%s], will settle for the absolute path at [%s]",
					source.getPath(), f.getPath()), e);
			} else {
				this.log.warn(String.format(
					"Failed to find the canonical path for [%s], will settle for the absolute path at [%s]",
					source.getPath(), f.getPath()));
			}
			source = f;
		}

		settings.put(LocalSetting.ROOT.getLabel(), source.getAbsolutePath());
		settings.put(LocalSetting.COPY_CONTENT.getLabel(), isCopyContent(commandValues));
		settings.put(LocalSetting.IGNORE_EMPTY_FOLDERS.getLabel(),
			commandValues.isPresent(CLIParam.ignore_empty_folders));
		return LocalEngineProxy.commonConfigure(commandValues, settings);
	}

	@Override
	protected void postConfigure(OptionValues commandValues, Map<String, Object> settings)
		throws CalienteException {
		super.postConfigure(commandValues, settings);
	}

	@Override
	protected void postValidateSettings(Map<String, Object> settings) throws CalienteException {
		super.postValidateSettings(settings);
	}
}