package com.delta.cmsmf.launcher.cmis;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import com.armedia.cmf.engine.cmis.CmisSessionSetting;
import com.armedia.cmf.engine.cmis.CmisSetting;
import com.armedia.cmf.engine.cmis.exporter.CmisExportEngine;
import com.delta.cmsmf.cfg.CLIParam;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.launcher.AbstractCMSMFMain_export;

public class CMSMFMain_export extends AbstractCMSMFMain_export {

	private static final String ID_PREFIX = "id:";

	public CMSMFMain_export() throws Throwable {
		super(CmisExportEngine.getExportEngine());
	}

	@Override
	protected void customizeSettings(Map<String, Object> settings) throws CMSMFException {
		URI baseUri;
		// Ensure it has a trailing slash...this will be useful later
		try {
			// baseUri = new URI(String.format("%s/", this.server));
			baseUri = new URI(this.server);
		} catch (URISyntaxException e) {
			throw new CMSMFException(String.format("Bad URL for the the CMIS repository: [%s]", this.server), e);
		}
		baseUri = baseUri.normalize();
		final URL baseUrl;
		try {
			baseUrl = baseUri.toURL();
		} catch (MalformedURLException e) {
			throw new CMSMFException(String.format("Bad URL for the CMIS repository: [%s]", this.server), e);
		}

		String srcPath = CLIParam.source.getString();
		if (StringUtils.isEmpty(srcPath)) { throw new CMSMFException("Must provide the CMIS source path or ID"); }

		// If it has a leading slash, it's a path
		if (srcPath.startsWith("/")) {
			settings.put(CmisSetting.EXPORT_PATH.getLabel(), FilenameUtils.normalize(srcPath, true));
		} else
		// If it has a leading "id:", it's an object ID
		if (srcPath.startsWith(CMSMFMain_export.ID_PREFIX)) {
			srcPath = srcPath.substring(CMSMFMain_export.ID_PREFIX.length());
			if (StringUtils.isEmpty(srcPath)) { throw new CMSMFException("Must provide a non-empty CMIS object ID"); }
			settings.put(CmisSetting.EXPORT_ID.getLabel(), srcPath);
		} else {
			// If it's neither a path or an ID, it's a query
			settings.put(CmisSetting.EXPORT_QUERY.getLabel(), srcPath);
		}

		settings.put(CmisSessionSetting.ATOMPUB_URL.getLabel(), baseUrl);
		String repoName = CLIParam.repository.getString();
		if (!StringUtils.isBlank(repoName)) {
			settings.put(CmisSessionSetting.REPOSITORY_ID.getLabel(), repoName);
		}
	}
}