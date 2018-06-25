package com.armedia.caliente.cli.caliente.launcher.ucm;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.caliente.cfg.CLIParam;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.launcher.EngineInterface;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.importer.ImportEngine;
import com.armedia.caliente.engine.ucm.UcmSessionSetting;
import com.armedia.caliente.engine.ucm.UcmSessionSetting.SSLMode;
import com.armedia.caliente.engine.ucm.UcmSetting;
import com.armedia.caliente.engine.ucm.exporter.UcmExportEngine;
import com.armedia.caliente.engine.ucm.importer.UcmImportEngine;

public class UcmEngineInterface extends EngineInterface {

	static boolean commonConfigure(OptionValues commandValues, Map<String, Object> settings)
		throws CalienteException {

		// TODO: Identify the server info
		String server = null;

		URI baseUri = URI.create(server);
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

		List<String> paths = new ArrayList<>();

		for (String srcPath : commandValues.getAllStrings(CLIParam.source)) {
			if (StringUtils.isEmpty(srcPath)) { throw new CalienteException("Empty paths are not allowed"); }
			if (!srcPath.startsWith("/")) {
				srcPath = StringUtils.strip(srcPath);
			} else {
				srcPath = FilenameUtils.normalize(srcPath, true);
			}
			paths.add(srcPath);
		}

		settings.put(UcmSetting.SOURCE.getLabel(), UcmExportEngine.encodePathList(paths));

		return true;
	}

	public UcmEngineInterface() {
	}

	@Override
	public String getName() {
		return "ucm";
	}

	@Override
	public Set<String> getAliases() {
		return Collections.emptySet();
	}

	@Override
	protected ExportEngine<?, ?, ?, ?, ?, ?> getExportEngine() {
		return UcmExportEngine.getExportEngine();
	}

	@Override
	protected UcmExporter newExporter(ExportEngine<?, ?, ?, ?, ?, ?> engine) {
		return new UcmExporter(engine);
	}

	@Override
	protected ImportEngine<?, ?, ?, ?, ?, ?> getImportEngine() {
		return UcmImportEngine.getImportEngine();
	}

	@Override
	protected UcmImporter newImporter(ImportEngine<?, ?, ?, ?, ?, ?> engine) {
		return new UcmImporter(engine);
	}

	@Override
	public Collection<? extends LaunchClasspathHelper> getClasspathHelpers() {
		return Collections.emptyList();
	}

}