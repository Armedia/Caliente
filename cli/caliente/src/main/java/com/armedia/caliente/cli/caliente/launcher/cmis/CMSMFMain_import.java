package com.armedia.caliente.cli.caliente.launcher.cmis;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import com.armedia.caliente.cli.caliente.cfg.CLIParam;
import com.armedia.caliente.cli.caliente.cfg.Setting;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.launcher.AbstractCMSMFMain_import;
import com.armedia.caliente.engine.TransferSetting;
import com.armedia.caliente.engine.cmis.CmisSessionSetting;
import com.armedia.caliente.engine.cmis.importer.CmisImportEngine;
import com.armedia.commons.utilities.Tools;

public class CMSMFMain_import extends AbstractCMSMFMain_import {

	public CMSMFMain_import() throws Throwable {
		super(CmisImportEngine.getImportEngine());
	}

	@Override
	protected void customizeSettings(Map<String, Object> settings) throws CalienteException {
		if (this.server == null) { throw new CalienteException(
			"Must provide the base URL where the CMIS repository may be accessed"); }

		URI baseUri;
		// Ensure it has a trailing slash...this will be useful later
		try {
			baseUri = new URI(String.format("%s/", this.server));
		} catch (URISyntaxException e) {
			throw new CalienteException(String.format("Bad URL for the the CMIS repository: [%s]", this.server), e);
		}
		baseUri = baseUri.normalize();
		final URL baseUrl;
		try {
			baseUrl = baseUri.toURL();
		} catch (MalformedURLException e) {
			throw new CalienteException(String.format("Bad URL for the CMIS repository: [%s]", this.server), e);
		}

		settings.put(CmisSessionSetting.ATOMPUB_URL.getLabel(), baseUrl);
		if (this.user != null) {
			settings.put(CmisSessionSetting.USER.getLabel(), this.user);
		}
		if (this.password != null) {
			settings.put(CmisSessionSetting.PASSWORD.getLabel(), this.password);
		}
		// TODO: Make this a CLI setting
		String repoName = CLIParam.repository.getString("-default-");
		settings.put(CmisSessionSetting.REPOSITORY_ID.getLabel(), Tools.coalesce(repoName, "-default-"));
		settings.put(TransferSetting.EXCLUDE_TYPES.getLabel(), Setting.CMF_EXCLUDE_TYPES.getString(null));

		super.customizeSettings(settings);
	}
}