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
package com.armedia.caliente.cli.caliente.launcher.local;

import java.io.File;
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
import com.armedia.caliente.cli.filter.StringValueFilter;
import com.armedia.caliente.engine.exporter.ExportEngineFactory;
import com.armedia.caliente.engine.local.common.LocalSetting;
import com.armedia.caliente.engine.local.exporter.LocalExportEngine;
import com.armedia.caliente.engine.tools.LocalOrganizer;
import com.armedia.caliente.store.local.LocalContentStoreSetting;
import com.armedia.caliente.store.xml.StoreConfiguration;
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

	private static final Option VERSION_SCHEME = new OptionImpl() //
		.setLongOpt("version-scheme") //
		.setDescription("The version scheme to use (how version tags are presented in a filename)") //
		.setArgumentLimits(1) //
		.setArgumentName("schemeName") //
		.setValueFilter(new StringValueFilter(LocalExportEngine.VERSION_SCHEMES)) //
	;

	private static final Option VERSION_TAG_SEPARATOR = new OptionImpl() //
		.setLongOpt("version-tag-separator") //
		.setDescription("The string that separates the base filename from the version tag") //
		.setArgumentLimits(1) //
		.setArgumentName("string") //
	;

	private static final Option VERSION_LAYOUT = new OptionImpl() //
		.setLongOpt("version-layout") //
		.setDescription("The version layout to use (how files are organized on disk)") //
		.setArgumentLimits(1) //
		.setArgumentName("layoutName") //
		.setValueFilter(new StringValueFilter(LocalExportEngine.VERSION_LAYOUTS)) //
	;

	private static final OptionGroup OPTIONS = new OptionGroupImpl("Local Export") //
		.add(Exporter.COPY_CONTENT) //
		.add(Exporter.IGNORE_EMPTY_FOLDERS) //
		.add(Exporter.VERSION_SCHEME) //
		.add(Exporter.VERSION_TAG_SEPARATOR) //
		.add(Exporter.VERSION_LAYOUT) //
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

	@Override
	public void customizeContentStoreProperties(OptionValues commandValues, StoreConfiguration cfg) {
		super.customizeContentStoreProperties(commandValues, cfg);
		boolean ignoreDescriptor = !isCopyContent(commandValues);
		cfg.getSettings().put(LocalContentStoreSetting.IGNORE_DESCRIPTOR.getLabel(), String.valueOf(ignoreDescriptor));
	}

	/*
	protected File getContentFilesLocation() {
		if (isCopyContent()) { return super.getContentFilesLocation(); }
		return new File(BaseParam.source.getString());
	}
	*/

	@Override
	public String getContentOrganizerName(OptionValues commandValues) {
		if (commandValues.isPresent(Exporter.COPY_CONTENT)) { return null; }
		return LocalOrganizer.NAME;
	}

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
		source = Tools.canonicalize(source);

		settings.put(LocalSetting.ROOT.getLabel(), source.getAbsolutePath());
		settings.put(LocalSetting.COPY_CONTENT.getLabel(), isCopyContent(commandValues));
		settings.put(LocalSetting.IGNORE_EMPTY_FOLDERS.getLabel(),
			commandValues.isPresent(Exporter.IGNORE_EMPTY_FOLDERS));
		settings.put(LocalSetting.VERSION_SCHEME.getLabel(), commandValues.getString(Exporter.VERSION_SCHEME));
		settings.put(LocalSetting.VERSION_TAG_SEPARATOR.getLabel(),
			commandValues.getString(Exporter.VERSION_TAG_SEPARATOR));
		settings.put(LocalSetting.VERSION_LAYOUT.getLabel(), commandValues.getString(Exporter.VERSION_LAYOUT));
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