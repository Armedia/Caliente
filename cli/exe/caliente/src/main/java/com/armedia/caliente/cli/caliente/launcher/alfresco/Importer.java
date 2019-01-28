package com.armedia.caliente.cli.caliente.launcher.alfresco;

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
import com.armedia.caliente.cli.caliente.launcher.DynamicCommandOptions;
import com.armedia.caliente.cli.caliente.options.CLIGroup;
import com.armedia.caliente.engine.alfresco.bi.AlfSetting;
import com.armedia.caliente.engine.importer.ImportEngineFactory;

class Importer extends ImportCommandModule implements DynamicCommandOptions {

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
		.setValueSep(',') //
		.setDescription("The XML files that make up the Alfresco content model to use on import") //
	;

	private static final OptionGroup OPTIONS = new OptionGroupImpl("Alfresco BI Generator") //
		.add(Importer.ATTRIBUTE_MAP) //
		.add(Importer.CONTENT_MODEL) //
	;

	Importer(ImportEngineFactory<?, ?, ?, ?, ?, ?> engine) {
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

		settings.put(AlfSetting.ROOT.getLabel(), state.getBaseDataLocation().getAbsolutePath());
		settings.put(AlfSetting.CONTENT.getLabel(), state.getContentStoreLocation().toString());

		if (!commandValues.isPresent(Importer.CONTENT_MODEL)) {
			throw new CalienteException(
				"No content models were given - these are required in order to properly generate the Alfresco metadata");
		}
		settings.put(AlfSetting.CONTENT_MODEL.getLabel(), commandValues.getStrings(Importer.CONTENT_MODEL));

		if (commandValues.isPresent(Importer.ATTRIBUTE_MAP)) {
			settings.put(AlfSetting.ATTRIBUTE_MAPPING.getLabel(), commandValues.getString(Importer.ATTRIBUTE_MAP));
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
	public void getDynamicOptions(String engine, OptionScheme scheme) {
		scheme //
			.addGroup(CLIGroup.IMPORT_COMMON) //
			.addGroup(Importer.OPTIONS) //
		;
	}
}