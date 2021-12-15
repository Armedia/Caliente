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

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.cli.caliente.cfg.CalienteState;
import com.armedia.caliente.cli.caliente.command.ExportCommandModule;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.launcher.DynamicCommandOptions;
import com.armedia.caliente.cli.caliente.options.CLIGroup;
import com.armedia.caliente.cli.caliente.options.CLIParam;
import com.armedia.caliente.engine.exporter.ExportEngineFactory;
import com.armedia.caliente.engine.local.common.LocalCaseFolding;
import com.armedia.caliente.engine.local.common.LocalSetting;
import com.armedia.caliente.engine.local.exporter.LocalExportEngine;
import com.armedia.caliente.engine.tools.LocalOrganizer;
import com.armedia.caliente.store.local.LocalContentStoreSetting;
import com.armedia.caliente.store.xml.StoreConfiguration;
import com.armedia.commons.utilities.cli.Option;
import com.armedia.commons.utilities.cli.OptionGroup;
import com.armedia.commons.utilities.cli.OptionGroupImpl;
import com.armedia.commons.utilities.cli.OptionImpl;
import com.armedia.commons.utilities.cli.OptionScheme;
import com.armedia.commons.utilities.cli.OptionValues;
import com.armedia.commons.utilities.cli.filter.EnumValueFilter;
import com.armedia.commons.utilities.cli.filter.StringValueFilter;

class Exporter extends ExportCommandModule implements DynamicCommandOptions {

	private static final Option COPY_CONTENT = new OptionImpl() //
		.setLongOpt("copy-content") //
		.setDescription("Enable the copying of content for the Local engine") //
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

	private static final Option BLIND_MODE = new OptionImpl() //
		.setLongOpt("blind-mode") //
		.setDescription(
			"The case folding mode to use when blind-scanning the source content (only applicable when using the JDBC engine)") //
		.setArgumentLimits(1) //
		.setArgumentName("caseFoldingMode") //
		.setValueFilter(new EnumValueFilter<>(false, LocalCaseFolding.class)) //
	;

	private static final OptionGroup OPTIONS = new OptionGroupImpl("Local Export") //
		.add(Exporter.COPY_CONTENT) //
		.add(Exporter.BLIND_MODE) //
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

		String source = commandValues.getString(CLIParam.from);
		if (StringUtils.isEmpty(source)) { throw new CalienteException("Must specify a source to export from"); }

		settings.put(LocalSetting.SOURCE.getLabel(), source);
		settings.put(LocalSetting.COPY_CONTENT.getLabel(), isCopyContent(commandValues));
		settings.put(LocalSetting.VERSION_SCHEME.getLabel(), commandValues.getString(Exporter.VERSION_SCHEME));
		settings.put(LocalSetting.VERSION_TAG_SEPARATOR.getLabel(),
			commandValues.getString(Exporter.VERSION_TAG_SEPARATOR));
		settings.put(LocalSetting.VERSION_LAYOUT.getLabel(), commandValues.getString(Exporter.VERSION_LAYOUT));

		if (commandValues.isPresent(Exporter.BLIND_MODE)) {
			settings.put(LocalSetting.BLIND_MODE.getLabel(),
				commandValues.getEnum(LocalCaseFolding.class, Exporter.BLIND_MODE));
			settings.put(LocalSetting.MINIMAL_DISK_ACCESS.getLabel(), Boolean.TRUE);
		}
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