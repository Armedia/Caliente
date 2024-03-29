/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.cli.caliente.launcher.alfresco;

import java.util.Map;

import com.armedia.caliente.cli.caliente.cfg.CalienteState;
import com.armedia.caliente.cli.caliente.command.ImportCommandModule;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.launcher.DynamicCommandOptions;
import com.armedia.caliente.cli.caliente.options.CLIGroup;
import com.armedia.caliente.engine.alfresco.bi.AlfSetting;
import com.armedia.caliente.engine.importer.ImportEngineFactory;
import com.armedia.commons.utilities.cli.Option;
import com.armedia.commons.utilities.cli.OptionGroup;
import com.armedia.commons.utilities.cli.OptionGroupImpl;
import com.armedia.commons.utilities.cli.OptionImpl;
import com.armedia.commons.utilities.cli.OptionScheme;
import com.armedia.commons.utilities.cli.OptionValues;

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

	private static final Option WITH_INGESTION_INDEX = new OptionImpl() //
		.setLongOpt("with-ingestion-index") //
		.setArgumentLimits(0, 0) //
		.setDescription("Indicate whether an ingestion index containing all ingested object IDs should be rendered") //
	;

	private static final OptionGroup OPTIONS = new OptionGroupImpl("Alfresco BI Generator") //
		.add(Importer.ATTRIBUTE_MAP) //
		.add(Importer.CONTENT_MODEL) //
		.add(Importer.WITH_INGESTION_INDEX) //
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
		settings.put(AlfSetting.GENERATE_INGESTION_INDEX.getLabel(),
			commandValues.isPresent(Importer.WITH_INGESTION_INDEX));

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