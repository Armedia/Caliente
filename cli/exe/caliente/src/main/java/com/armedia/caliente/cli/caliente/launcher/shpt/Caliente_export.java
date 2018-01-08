package com.armedia.caliente.cli.caliente.launcher.shpt;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.armedia.caliente.cli.caliente.cfg.CLIParam;
import com.armedia.caliente.cli.caliente.cfg.Setting;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.launcher.AbstractCalienteModule_export;
import com.armedia.caliente.engine.exporter.ExportEngineListener;
import com.armedia.caliente.engine.sharepoint.ShptSetting;
import com.armedia.caliente.engine.sharepoint.exporter.ShptExportEngine;
import com.armedia.commons.utilities.FileNameTools;

public class Caliente_export extends AbstractCalienteModule_export implements ExportEngineListener {

	public Caliente_export() throws Throwable {
		super(ShptExportEngine.getExportEngine());
	}

	@Override
	protected void customizeSettings(Map<String, Object> settings) throws CalienteException {
		URI baseUri;
		// Ensure it has a trailing slash...this will be useful later
		try {
			baseUri = new URI(String.format("%s/", this.server));
		} catch (URISyntaxException e) {
			throw new CalienteException(String.format("Bad URL for Sharepoint: [%s]", this.server), e);
		}
		baseUri = baseUri.normalize();
		final URL baseUrl;
		try {
			baseUrl = baseUri.toURL();
		} catch (MalformedURLException e) {
			throw new CalienteException(String.format("Bad URL for Sharepoint: [%s]", this.server), e);
		}

		String srcPath = CLIParam.source.getString();
		if (srcPath == null) { throw new CalienteException("Must provide the name of the sharepoint site to export"); }
		List<String> l = FileNameTools.tokenize(srcPath, '/');
		if (l.isEmpty()) { throw new CalienteException("Must provide the name of the sharepoint site to export"); }
		final String site = l.get(0);
		if (StringUtils
			.isEmpty(site)) { throw new CalienteException("Must provide the name of the sharepoint site to export"); }

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
			throw new CalienteException("Bad base URL", e);
		}
		settings.put(ShptSetting.PATH.getLabel(),
			String.format("%s/%s", StringUtils.isEmpty(srcPrefix) ? "" : String.format("/%s", srcPrefix), srcPath));
	}

	@Override
	protected String getUserSetting() {
		return ShptSetting.USER.getLabel();
	}

	@Override
	protected String getPasswordSetting() {
		return ShptSetting.PASSWORD.getLabel();
	}

	@Override
	protected String getDomainSetting() {
		return ShptSetting.DOMAIN.getLabel();
	}
}