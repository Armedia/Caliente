package com.armedia.caliente.cli.caliente.launcher.cmis;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.caliente.cfg.CalienteState;
import com.armedia.caliente.cli.caliente.command.ExportCommandModule;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.launcher.DynamicCommandOptions;
import com.armedia.caliente.cli.caliente.options.CLIGroup;
import com.armedia.caliente.engine.cmis.CmisSessionSetting;
import com.armedia.caliente.engine.exporter.ExportEngineFactory;

class Exporter extends ExportCommandModule implements DynamicCommandOptions {
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

		final String server = null;

		URI baseUri;
		// Ensure it has a trailing slash...this will be useful later
		try {
			// baseUri = new URI(String.format("%s/", this.server));
			baseUri = new URI(server);
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

		settings.put(CmisSessionSetting.ATOMPUB_URL.getLabel(), baseUrl);
		String repoName = null;
		if (!StringUtils.isBlank(repoName)) {
			settings.put(CmisSessionSetting.REPOSITORY_ID.getLabel(), repoName);
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
		;
	}
}