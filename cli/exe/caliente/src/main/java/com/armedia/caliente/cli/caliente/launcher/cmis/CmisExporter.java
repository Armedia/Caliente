package com.armedia.caliente.cli.caliente.launcher.cmis;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.caliente.cfg.CalienteExportOptions;
import com.armedia.caliente.cli.caliente.command.ExportCommandModule;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.engine.cmis.CmisSessionSetting;
import com.armedia.caliente.engine.cmis.CmisSetting;
import com.armedia.caliente.engine.exporter.ExportEngine;

class CmisExporter extends ExportCommandModule {
	CmisExporter(ExportEngine<?, ?, ?, ?, ?, ?> engine) {
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
	protected boolean preConfigure(OptionValues commandValues, Map<String, Object> settings) throws CalienteException {
		return super.preConfigure(commandValues, settings);
	}

	@Override
	protected boolean doConfigure(OptionValues commandValues, Map<String, Object> settings) throws CalienteException {
		if (!super.doConfigure(commandValues, settings)) { return false; }

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

		String srcPath = commandValues.getString(CalienteExportOptions.SOURCE);
		if (StringUtils.isEmpty(srcPath)) { throw new CalienteException("Must provide the CMIS source path or ID"); }

		// If it has a leading slash, it's a path
		if (srcPath.startsWith("/")) {
			settings.put(CmisSetting.EXPORT_PATH.getLabel(), FilenameUtils.normalize(srcPath, true));
		} else
		// If it has a leading "id:", it's an object ID
		if (srcPath.startsWith(CmisEngineInterface.ID_PREFIX)) {
			srcPath = srcPath.substring(CmisEngineInterface.ID_PREFIX.length());
			if (StringUtils
				.isEmpty(srcPath)) { throw new CalienteException("Must provide a non-empty CMIS object ID"); }
			settings.put(CmisSetting.EXPORT_ID.getLabel(), srcPath);
		} else {
			// If it's neither a path or an ID, it's a query
			settings.put(CmisSetting.EXPORT_QUERY.getLabel(), srcPath);
		}

		settings.put(CmisSessionSetting.ATOMPUB_URL.getLabel(), baseUrl);
		String repoName = null;
		if (!StringUtils.isBlank(repoName)) {
			settings.put(CmisSessionSetting.REPOSITORY_ID.getLabel(), repoName);
		}

		return true;
	}

	@Override
	protected void postConfigure(OptionValues commandValues, Map<String, Object> settings) throws CalienteException {
		super.postConfigure(commandValues, settings);
	}

	@Override
	protected void postValidateSettings(Map<String, Object> settings) throws CalienteException {
		super.postValidateSettings(settings);
	}
}