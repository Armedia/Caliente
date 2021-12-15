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
package com.armedia.caliente.cli.caliente.launcher.cmis;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import org.apache.chemistry.opencmis.commons.enums.BindingType;

import com.armedia.caliente.cli.caliente.cfg.CalienteState;
import com.armedia.caliente.cli.caliente.command.ImportCommandModule;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.launcher.DynamicCommandOptions;
import com.armedia.caliente.cli.caliente.options.CLIGroup;
import com.armedia.caliente.cli.caliente.options.CLIParam;
import com.armedia.caliente.engine.TransferSetting;
import com.armedia.caliente.engine.cmis.CmisSessionSetting;
import com.armedia.caliente.engine.importer.ImportEngineFactory;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.cli.Option;
import com.armedia.commons.utilities.cli.OptionGroup;
import com.armedia.commons.utilities.cli.OptionGroupImpl;
import com.armedia.commons.utilities.cli.OptionImpl;
import com.armedia.commons.utilities.cli.OptionScheme;
import com.armedia.commons.utilities.cli.OptionValues;

class Importer extends ImportCommandModule implements DynamicCommandOptions {
	static final Option DELETE_ON_FAIL = new OptionImpl() //
		.setLongOpt("delete-on-fail") //
		.setDescription("Delete created documents on fail, to avoid partial ingestions") //
	;

	private static final OptionGroup CMIS_IMPORT_OPTIONS = new OptionGroupImpl("CMIS Import Configuration") //
		.add(Importer.DELETE_ON_FAIL) //
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

		final String server = commandValues.getString(CLIParam.server);

		URI baseUri;
		// Ensure it has a trailing slash...this will be useful later
		try {
			baseUri = new URI(String.format("%s/", server));
		} catch (URISyntaxException e) {
			throw new CalienteException(String.format("Bad URL for the the CMIS repository: [%s]", server), e);
		}
		baseUri = baseUri.normalize();
		final URL baseUrl;
		try {
			baseUrl = baseUri.toURL();
		} catch (MalformedURLException e) {
			throw new CalienteException(String.format("Bad URL for the CMIS repository: [%s]", server), e);
		}

		String user = commandValues.getString(CLIParam.user);
		String password = commandValues.getString(CLIParam.password);

		settings.put(CmisSessionSetting.URL.getLabel(), baseUrl);
		if (user != null) {
			settings.put(CmisSessionSetting.USER.getLabel(), user);
		}
		if (password != null) {
			settings.put(CmisSessionSetting.PASSWORD.getLabel(), password);
		}
		String repoName = commandValues.getString(CLIParam.domain, "-default-");
		settings.put(CmisSessionSetting.REPOSITORY_ID.getLabel(), Tools.coalesce(repoName, "-default-"));
		if (commandValues.hasValues(CLIParam.only_types)) {
			settings.put(TransferSetting.ONLY_TYPES.getLabel(), commandValues.getStrings(CLIParam.only_types));
		} else if (commandValues.hasValues(CLIParam.except_types)) {
			settings.put(TransferSetting.EXCEPT_TYPES.getLabel(), commandValues.getStrings(CLIParam.except_types));
		}

		BindingType bindingType = commandValues.getEnum(BindingType.class, EngineInterface.BINDING_TYPE);
		settings.put(CmisSessionSetting.BINDING_TYPE.getLabel(), bindingType.value());

		if (commandValues.isDefined(Importer.DELETE_ON_FAIL)) {
			settings.put(CmisSessionSetting.DELETE_ON_FAIL.getLabel(), true);
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
	public void getDynamicOptions(String engine, OptionScheme command) {
		command //
			.addGroup(CLIGroup.IMPORT_COMMON) //
			.addGroup(Importer.CMIS_IMPORT_OPTIONS) //
		;

	}
}