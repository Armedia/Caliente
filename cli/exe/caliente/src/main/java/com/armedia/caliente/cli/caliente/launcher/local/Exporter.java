package com.armedia.caliente.cli.caliente.launcher.local;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionGroup;
import com.armedia.caliente.cli.OptionGroupImpl;
import com.armedia.caliente.cli.OptionImpl;
import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.caliente.cfg.CalienteState;
import com.armedia.caliente.cli.caliente.command.ExportCommandModule;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.launcher.DynamicCommandOptions;
import com.armedia.caliente.cli.caliente.options.CLIGroup;
import com.armedia.caliente.cli.caliente.options.CLIParam;
import com.armedia.caliente.engine.exporter.ExportEngineFactory;
import com.armedia.caliente.engine.local.common.LocalSetting;
import com.armedia.commons.utilities.Tools;

class Exporter extends ExportCommandModule implements DynamicCommandOptions {

	private static final Option COPY_CONTENT = new OptionImpl() //
		.setLongOpt("copy-content") //
		.setDescription("Enable the copying of content for the Local engine") //
	;

	private static final Option IGNORE_EMPTY_FOLDERS = new OptionImpl() //
		.setLongOpt("ignore-empty-folders") //
		.setDescription("Ignore empty folders during extraction") //
	;

	private static final OptionGroup OPTIONS = new OptionGroupImpl("Local Export") //
		.add(Exporter.COPY_CONTENT) //
	;

	Exporter(ExportEngineFactory<?, ?, ?, ?, ?, ?> engine) {
		super(engine);
	}

	@Override
	public boolean isContentStreamsExternal(OptionValues commandValues) {
		return !commandValues.isPresent(Exporter.COPY_CONTENT);
	}

	@Override
	protected boolean preInitialize(CalienteState state, Map<String, Object> settings) {
		return super.preInitialize(state, settings);
	}

	@Override
	protected boolean doInitialize(CalienteState state, Map<String, Object> settings) {
		return super.doInitialize(state, settings);
	}

	@Override
	protected boolean postInitialize(CalienteState state, Map<String, Object> settings) {
		return super.postInitialize(state, settings);
	}

	@Override
	protected void preValidateSettings(CalienteState state, Map<String, Object> settings) throws CalienteException {
		super.preValidateSettings(state, settings);
	}

	@Override
	protected boolean preConfigure(CalienteState state, OptionValues commandValues, Map<String, Object> settings)
		throws CalienteException {
		return super.preConfigure(state, commandValues, settings);
	}

	protected boolean isCopyContent(OptionValues commandValues) {
		return commandValues.isPresent(Exporter.COPY_CONTENT) && !commandValues.isPresent(CLIParam.skip_content);
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
	
	protected String getContentOrganizerName() {
		return LocalOrganizer.NAME;
	}
	*/

	@Override
	protected boolean doConfigure(CalienteState state, OptionValues commandValues, Map<String, Object> settings)
		throws CalienteException {
		if (!super.doConfigure(state, commandValues, settings)) { return false; }

		File source = Tools.canonicalize(new File(commandValues.getString(CLIParam.from)));

		// Make sure a source has been specified
		if (source == null) { throw new CalienteException("Must specify a source to export from"); }
		if (!source.exists()) {
			throw new CalienteException(String.format("The specified source at [%s] does not exist", source.getPath()));
		}
		if (!source.exists()) {
			throw new CalienteException(String.format("The specified source at [%s] does not exist", source.getPath()));
		}
		if (!source.isDirectory()) {
			throw new CalienteException(
				String.format("The specified source at [%s] is not a directory", source.getPath()));
		}
		if (!source.canRead()) {
			throw new CalienteException(
				String.format("The specified source at [%s] is not readable", source.getPath()));
		}
		try {
			File f = source.getCanonicalFile();
			source = f;
		} catch (IOException e) {
			File f = source.getAbsoluteFile();
			String fmt = "Failed to find the canonical path for [{}], will settle for the absolute path at [{}]";
			if (this.log.isTraceEnabled()) {
				this.log.warn(fmt, source.getPath(), f.getPath(), e);
			} else {
				this.log.warn(fmt, source.getPath(), f.getPath());
			}
			source = f;
		}

		settings.put(LocalSetting.ROOT.getLabel(), source.getAbsolutePath());
		settings.put(LocalSetting.COPY_CONTENT.getLabel(), isCopyContent(commandValues));
		settings.put(LocalSetting.IGNORE_EMPTY_FOLDERS.getLabel(),
			commandValues.isPresent(Exporter.IGNORE_EMPTY_FOLDERS));
		return EngineInterface.commonConfigure(commandValues, settings);
	}

	@Override
	protected void postConfigure(CalienteState state, OptionValues commandValues, Map<String, Object> settings)
		throws CalienteException {
		super.postConfigure(state, commandValues, settings);
	}

	@Override
	protected void postValidateSettings(CalienteState state, Map<String, Object> settings) throws CalienteException {
		super.postValidateSettings(state, settings);
	}

	@Override
	public void getDynamicOptions(String engine, OptionScheme scheme) {
		scheme //
			.addGroup(CLIGroup.EXPORT_COMMON) //
			.addGroup(Exporter.OPTIONS) //
		;
	}
}