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
package com.armedia.caliente.cli.caliente.launcher.shpt;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.cli.caliente.cfg.CalienteState;
import com.armedia.caliente.cli.caliente.command.ExportCommandModule;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.launcher.DynamicCommandOptions;
import com.armedia.caliente.cli.caliente.options.CLIGroup;
import com.armedia.caliente.cli.caliente.options.CLIParam;
import com.armedia.caliente.engine.exporter.ExportEngineFactory;
import com.armedia.caliente.engine.sharepoint.ShptSetting;
import com.armedia.commons.utilities.EncodedString;
import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.cli.Option;
import com.armedia.commons.utilities.cli.OptionGroup;
import com.armedia.commons.utilities.cli.OptionGroupImpl;
import com.armedia.commons.utilities.cli.OptionImpl;
import com.armedia.commons.utilities.cli.OptionScheme;
import com.armedia.commons.utilities.cli.OptionValues;

class Exporter extends ExportCommandModule implements DynamicCommandOptions {

	private static final Option SOURCE_PREFIX = new OptionImpl() //
		.setLongOpt("source-prefix") //
		.setArgumentLimits(1) //
		.setArgumentName("prefix") //
		.setDescription("The prefix to pre-pend to Sharepoint source paths (i.e. /sites is the default)") //
		.setDefault("/sites") //
	;

	private static final OptionGroup OPTIONS = new OptionGroupImpl("SharePoint Export") //
		.add(Exporter.SOURCE_PREFIX) //
	;

	Exporter(ExportEngineFactory<?, ?, ?, ?, ?, ?> engine) {
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
		final String user = commandValues.getString(CLIParam.user);
		final String domain = commandValues.getString(CLIParam.domain);
		final String password = commandValues.getString(CLIParam.password);

		settings.put(ShptSetting.USER.getLabel(), user);
		settings.put(ShptSetting.DOMAIN.getLabel(), domain);

		if (password != null) {
			try {
				settings.put(ShptSetting.PASSWORD.getLabel(),
					EncodedString.from(getCrypto().decrypt(password), getCrypto()));
			} catch (Exception e) {
				throw new CalienteException("Failed to safeguard the password in an encrypted object", e);
			}
		}

		URI baseUri;
		// Ensure it has a trailing slash...this will be useful later
		try {
			baseUri = new URI(String.format("%s/", server));
		} catch (URISyntaxException e) {
			throw new CalienteException(String.format("Bad URL for Sharepoint: [%s]", server), e);
		}
		baseUri = baseUri.normalize();
		final URL baseUrl;
		try {
			baseUrl = baseUri.toURL();
		} catch (MalformedURLException e) {
			throw new CalienteException(String.format("Bad URL for Sharepoint: [%s]", server), e);
		}

		String srcPath = commandValues.getString(CLIParam.from);
		if (srcPath == null) { throw new CalienteException("Must provide the title of the sharepoint site to export"); }
		List<String> l = FileNameTools.tokenize(srcPath, '/');
		if (l.isEmpty()) { throw new CalienteException("Must provide the title of the sharepoint site to export"); }
		final String site = l.get(0);
		if (StringUtils.isEmpty(site)) {
			throw new CalienteException("Must provide the title of the sharepoint site to export");
		}

		srcPath = FileNameTools.reconstitute(l, false, false, '/');

		l = FileNameTools.tokenize(commandValues.getString(Exporter.SOURCE_PREFIX, "/"));
		final String srcPrefix;
		if (l.isEmpty()) {
			srcPrefix = "";
		} else {
			srcPrefix = FileNameTools.reconstitute(l, false, false, '/');
		}

		try {
			// We don't use a leading slash here in "sites" because the URL *SHOULD* contain a
			// trailing slash
			settings.put(ShptSetting.URL.getLabel(),
				new URL(baseUrl,
					String.format("%s%s", StringUtils.isEmpty(srcPrefix) ? "" : String.format("%s/", srcPrefix), site))
						.toString());
		} catch (MalformedURLException e) {
			throw new CalienteException("Bad base URL", e);
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
			.addGroup(CLIGroup.EXPORT_COMMON) //
			.addGroup(Exporter.OPTIONS) //
		;
	}
}