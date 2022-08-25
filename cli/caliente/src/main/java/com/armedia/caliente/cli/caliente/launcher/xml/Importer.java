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
package com.armedia.caliente.cli.caliente.launcher.xml;

import java.io.File;
import java.util.Map;

import com.armedia.caliente.cli.caliente.cfg.CalienteState;
import com.armedia.caliente.cli.caliente.command.ImportCommandModule;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.launcher.DynamicCommandOptions;
import com.armedia.caliente.cli.caliente.options.CLIGroup;
import com.armedia.caliente.cli.caliente.options.CLIParam;
import com.armedia.caliente.engine.importer.ImportEngineFactory;
import com.armedia.caliente.engine.xml.common.XmlSetting;
import com.armedia.caliente.store.CmfContentOrganizer;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.cli.Option;
import com.armedia.commons.utilities.cli.OptionGroup;
import com.armedia.commons.utilities.cli.OptionGroupImpl;
import com.armedia.commons.utilities.cli.OptionImpl;
import com.armedia.commons.utilities.cli.OptionScheme;
import com.armedia.commons.utilities.cli.OptionValues;
import com.armedia.commons.utilities.cli.filter.StringValueFilter;

class Importer extends ImportCommandModule implements DynamicCommandOptions {
	private static final Option ORGANIZER = new OptionImpl() //
		.setLongOpt("xml-organizer") //
		.setArgumentLimits(1) //
		.setArgumentName("organizer-name") //
		.setValueFilter(new StringValueFilter(CmfContentOrganizer.getNames())) //
		.setDescription(
			"The name for the content organizer to use for the XML file structure (default: same as used by the configured content store)") //
	;

	private static final Option AGGREGATE_DOCUMENTS = new OptionImpl() //
		.setLongOpt("aggregate-documents") //
		.setArgumentLimits(0, 0) //
		.setDescription(
			"Instead of rendering a singular XML companion file for each Document, render a single, large index for all Documents") //
	;

	private static final Option AGGREGATE_FOLDERS = new OptionImpl() //
		.setLongOpt("aggregate-folders") //
		.setArgumentLimits(0, 0) //
		.setDescription(
			"Instead of rendering a singular XML companion file for each Folder, render a single, large index for all Folders") //
	;

	private static final Option ENCODING = new OptionImpl() //
		.setLongOpt("encoding") //
		.setArgumentLimits(1) //
		.setDescription("Use the given XML encoding instead of the default") //
		.setDefault(XmlSetting.ENCODING.getDefaultValue() //
		);

	private static final OptionGroup OPTIONS = new OptionGroupImpl("XML Import") //
		.add(Importer.ORGANIZER) //
		.add(Importer.AGGREGATE_DOCUMENTS) //
		.add(Importer.AGGREGATE_FOLDERS) //
		.add(Importer.ENCODING) //
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

		String target = commandValues.getString(CLIParam.data);
		if (target == null) {
			target = ".";
		}
		final File targetDir = Tools.canonicalize(new File(target));
		targetDir.mkdirs();
		if (!targetDir.exists()) {
			throw new CalienteException(
				String.format("The target directory [%s] does not exist, and could not be created", targetDir));
		}
		if (!targetDir.isDirectory()) {
			throw new CalienteException(
				String.format("A non-directory already exists at the location [%s] - can't continue", targetDir));
		}

		settings.put(XmlSetting.ROOT.getLabel(), targetDir.getAbsolutePath());
		settings.put(XmlSetting.ORGANIZER.getLabel(), commandValues.getString(Importer.ORGANIZER));
		settings.put(XmlSetting.AGGREGATE_FOLDERS.getLabel(), commandValues.isPresent(Importer.AGGREGATE_FOLDERS));
		settings.put(XmlSetting.AGGREGATE_DOCUMENTS.getLabel(), commandValues.isPresent(Importer.AGGREGATE_DOCUMENTS));
		settings.put(XmlSetting.ENCODING.getLabel(), commandValues.getString(Importer.ENCODING));
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