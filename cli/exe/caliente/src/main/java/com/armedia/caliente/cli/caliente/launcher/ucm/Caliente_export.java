package com.armedia.caliente.cli.caliente.launcher.ucm;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.cli.caliente.cfg.CLIParam;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.launcher.AbstractCalienteModule_export;
import com.armedia.caliente.engine.ucm.UcmSessionSetting;
import com.armedia.caliente.engine.ucm.UcmSessionSetting.SSLMode;
import com.armedia.caliente.engine.ucm.UcmSetting;
import com.armedia.caliente.engine.ucm.exporter.UcmExportEngine;

public class Caliente_export extends AbstractCalienteModule_export {

	public Caliente_export() throws Throwable {
		super(UcmExportEngine.getExportEngine());
	}

	@Override
	protected void customizeSettings(Map<String, Object> settings) throws CalienteException {
		URI baseUri = URI.create(this.server);
		baseUri = baseUri.normalize();

		if (baseUri.isOpaque()) { throw new CalienteException(
			String.format("Bad URL format for UCM connectivity: [%s]", baseUri)); }

		String scheme = baseUri.getScheme().toLowerCase();
		boolean ssl = false;
		if ("idc".equalsIgnoreCase(scheme)) {
			ssl = false;
		} else if ("idcs".equalsIgnoreCase(scheme)) {
			ssl = true;
		} else {
			throw new CalienteException(String.format("Unknown URL scheme [%s] in [%s]", scheme, baseUri));
		}

		String host = baseUri.getHost();
		if (host == null) { throw new CalienteException(
			String.format("Bad URL format for UCM connectivity: [%s]", baseUri)); }
		settings.put(UcmSessionSetting.HOST.getLabel(), host);

		int port = baseUri.getPort();
		if (port > 0) {
			settings.put(UcmSessionSetting.PORT.getLabel(), port);
		}

		if (ssl) {
			settings.put(UcmSessionSetting.SSL_MODE.getLabel(), SSLMode.SERVER.name());
		}

		settings.put(UcmSessionSetting.USER.getLabel(), this.user);
		settings.put(UcmSessionSetting.PASSWORD.getLabel(), this.password);

		List<String> paths = new ArrayList<>();

		for (String srcPath : CLIParam.source.getAllString()) {
			if (StringUtils.isEmpty(srcPath)) { throw new CalienteException("Empty paths are not allowed"); }
			if (!srcPath.startsWith("/")) {
				srcPath = StringUtils.strip(srcPath);
			} else {
				srcPath = FilenameUtils.normalize(srcPath, true);
			}
			paths.add(srcPath);
		}

		settings.put(UcmSetting.SOURCE.getLabel(), UcmExportEngine.encodePathList(paths));
	}
}