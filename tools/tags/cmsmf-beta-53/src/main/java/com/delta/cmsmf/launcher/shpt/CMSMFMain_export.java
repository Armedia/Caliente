package com.delta.cmsmf.launcher.shpt;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.armedia.cmf.engine.exporter.ExportEngineListener;
import com.armedia.cmf.engine.sharepoint.ShptSetting;
import com.armedia.cmf.engine.sharepoint.exporter.ShptExportEngine;
import com.armedia.commons.utilities.FileNameTools;
import com.delta.cmsmf.cfg.CLIParam;
import com.delta.cmsmf.cfg.Setting;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.launcher.AbstractCMSMFMain_export;

public class CMSMFMain_export extends AbstractCMSMFMain_export implements ExportEngineListener {

	public CMSMFMain_export() throws Throwable {
		super(ShptExportEngine.getExportEngine());
	}

	@Override
	protected void customizeSettings(Map<String, Object> settings) throws CMSMFException {
		URI baseUri;
		// Ensure it has a trailing slash...this will be useful later
		try {
			baseUri = new URI(String.format("%s/", this.server));
		} catch (URISyntaxException e) {
			throw new CMSMFException(String.format("Bad URL for Sharepoint: [%s]", this.server), e);
		}
		baseUri = baseUri.normalize();
		final URL baseUrl;
		try {
			baseUrl = baseUri.toURL();
		} catch (MalformedURLException e) {
			throw new CMSMFException(String.format("Bad URL for Sharepoint: [%s]", this.server), e);
		}

		String srcPath = CLIParam.source.getString();
		if (srcPath == null) { throw new CMSMFException("Must provide the name of the sharepoint site to export"); }
		List<String> l = FileNameTools.tokenize(srcPath, '/');
		if (l.isEmpty()) { throw new CMSMFException("Must provide the name of the sharepoint site to export"); }
		final String site = l.get(0);
		if (StringUtils
			.isEmpty(site)) { throw new CMSMFException("Must provide the name of the sharepoint site to export"); }

		srcPath = FileNameTools.reconstitute(l, false, false, '/');

		l = FileNameTools.tokenize(Setting.SHPT_SOURCE_PREFIX.getString(), '/');
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
			throw new CMSMFException("Bad base URL", e);
		}
		if (this.user != null) {
			settings.put(ShptSetting.USER.getLabel(), this.user);
		}
		if (this.password != null) {
			settings.put(ShptSetting.PASSWORD.getLabel(), this.password);
		}
		settings.put(ShptSetting.PATH.getLabel(),
			String.format("%s/%s", StringUtils.isEmpty(srcPrefix) ? "" : String.format("/%s", srcPrefix), srcPath));
	}
}