package com.armedia.caliente.cli.caliente.launcher.cmis;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.caliente.cfg.Setting;
import com.armedia.caliente.cli.caliente.command.ImportCommandModule;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.engine.TransferSetting;
import com.armedia.caliente.engine.cmis.CmisSessionSetting;
import com.armedia.caliente.engine.importer.ImportEngine;
import com.armedia.commons.utilities.Tools;

class CmisImporter extends ImportCommandModule {
	CmisImporter(ImportEngine<?, ?, ?, ?, ?, ?> engine) {
		super(engine);
	}

	@Override
	protected boolean preInitialize(Map<String, Object> settings) {
		return super.preInitialize(settings);
	}

	@Override
	protected boolean doInitialize(Map<String, Object> settings) {
		return super.doInitialize(settings);
	}

	@Override
	protected boolean postInitialize(Map<String, Object> settings) {
		return super.postInitialize(settings);
	}

	@Override
	protected void preValidateSettings(Map<String, Object> settings) throws CalienteException {
		super.preValidateSettings(settings);
	}

	@Override
	protected boolean preConfigure(OptionValues commandValues, Map<String, Object> settings)
		throws CalienteException {
		return super.preConfigure(commandValues, settings);
	}

	@Override
	protected boolean doConfigure(OptionValues commandValues, Map<String, Object> settings)
		throws CalienteException {
		if (!super.doConfigure(commandValues, settings)) { return false; }

		final String server = null;

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

		String user = null;
		String password = null;

		settings.put(CmisSessionSetting.ATOMPUB_URL.getLabel(), baseUrl);
		if (user != null) {
			settings.put(CmisSessionSetting.USER.getLabel(), user);
		}
		if (password != null) {
			settings.put(CmisSessionSetting.PASSWORD.getLabel(), password);
		}
		// TODO: Make this a CLI setting
		String repoName = "-default-";
		settings.put(CmisSessionSetting.REPOSITORY_ID.getLabel(), Tools.coalesce(repoName, "-default-"));
		settings.put(TransferSetting.EXCLUDE_TYPES.getLabel(), Setting.CMF_EXCLUDE_TYPES.getString(null));

		return true;
	}

	@Override
	protected void postConfigure(OptionValues commandValues, Map<String, Object> settings)
		throws CalienteException {
		super.postConfigure(commandValues, settings);
	}

	@Override
	protected void postValidateSettings(Map<String, Object> settings) throws CalienteException {
		super.postValidateSettings(settings);
	}
}