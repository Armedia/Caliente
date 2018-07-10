package com.armedia.caliente.cli.caliente.launcher.alfresco;

import java.io.File;
import java.util.Map;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionGroup;
import com.armedia.caliente.cli.OptionGroupImpl;
import com.armedia.caliente.cli.OptionImpl;
import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.caliente.cfg.CalienteState;
import com.armedia.caliente.cli.caliente.command.ImportCommandModule;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.launcher.DynamicOptions;
import com.armedia.caliente.cli.caliente.options.CLIGroup;
import com.armedia.caliente.cli.caliente.options.CLIParam;
import com.armedia.caliente.engine.alfresco.bi.AlfSetting;
import com.armedia.caliente.engine.importer.ImportEngine;
import com.armedia.commons.utilities.Tools;

class AlfrescoImporter extends ImportCommandModule implements DynamicOptions {

	private static final Option ATTRIBUTE_MAP = new OptionImpl() //
		.setLongOpt("attribute-map") //
		.setArgumentLimits(1) //
		.setArgumentName("mapping-file") //
		.setDescription(
			"The XML file that describes how attributes should be mapped from the source data into Alfresco attributes") //
	;

	private static final Option CONTENT_MODEL = new OptionImpl() //
		.setLongOpt("content-model") //
		.setArgumentLimits(1, -1) //
		.setArgumentName("content-model-file") //
		.setDescription("The XML files that make up the Alfresco content model to use on import") //
	;

	private static final OptionGroup OPTIONS = new OptionGroupImpl("Alfresco BI Generator Options") //
		.add(AlfrescoImporter.ATTRIBUTE_MAP) //
		.add(AlfrescoImporter.CONTENT_MODEL) //
	;

	AlfrescoImporter(ImportEngine<?, ?, ?, ?, ?, ?> engine) {
		super(engine);
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

	@Override
	protected boolean doConfigure(CalienteState state, OptionValues commandValues, Map<String, Object> settings)
		throws CalienteException {
		if (!super.doConfigure(state, commandValues, settings)) { return false; }

		String target = commandValues.getString(CLIParam.source);
		if (target == null) {
			target = ".";
		}
		final File targetDir = Tools.canonicalize(new File(target));
		targetDir.mkdirs();
		if (!targetDir.exists()) { throw new CalienteException(
			String.format("The target directory [%s] does not exist, and could not be created", targetDir)); }
		if (!targetDir.isDirectory()) { throw new CalienteException(
			String.format("A non-directory already exists at the location [%s] - can't continue", targetDir)); }

		settings.put(AlfSetting.ROOT.getLabel(), targetDir.getAbsolutePath());
		settings.put(AlfSetting.DB.getLabel(), state.getObjectStoreLocation().toString());
		settings.put(AlfSetting.CONTENT.getLabel(), state.getContentStoreLocation().toString());

		if (!commandValues.isPresent(AlfrescoImporter.CONTENT_MODEL)) { throw new CalienteException(
			"No content models were given - these are required in order to properly generate the Alfresco metadata"); }
		settings.put(AlfSetting.CONTENT_MODEL.getLabel(),
			Tools.joinCSVEscaped(commandValues.getAllStrings(AlfrescoImporter.CONTENT_MODEL)));

		if (commandValues.isPresent(AlfrescoImporter.ATTRIBUTE_MAP)) {
			settings.put(AlfSetting.ATTRIBUTE_MAPPING.getLabel(),
				commandValues.getString(AlfrescoImporter.ATTRIBUTE_MAP));
		}

		return true;
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
	public void getDynamicOptions(OptionScheme command) {
		command //
			.addGroup(CLIGroup.IMPORT_COMMON) //
			.addGroup(AlfrescoImporter.OPTIONS) //
		;
	}
}