package com.armedia.caliente.cli.caliente.launcher.shpt;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.caliente.cfg.CLIParam;
import com.armedia.caliente.cli.caliente.cfg.CalienteState;
import com.armedia.caliente.cli.caliente.command.ExportCommandModule;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.options.CLIOptions;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.sharepoint.ShptSetting;
import com.armedia.commons.utilities.FileNameTools;

class ShptExporter extends ExportCommandModule {
	ShptExporter(ExportEngine<?, ?, ?, ?, ?, ?> engine) {
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

		// TODO: Read this
		final String server = null;

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

		String srcPath = commandValues.getString(CLIOptions.SOURCE);
		if (srcPath == null) { throw new CalienteException("Must provide the title of the sharepoint site to export"); }
		List<String> l = FileNameTools.tokenize(srcPath, '/');
		if (l.isEmpty()) { throw new CalienteException("Must provide the title of the sharepoint site to export"); }
		final String site = l.get(0);
		if (StringUtils
			.isEmpty(site)) { throw new CalienteException("Must provide the title of the sharepoint site to export"); }

		srcPath = FileNameTools.reconstitute(l, false, false, '/');

		l = FileNameTools.tokenize(commandValues.getString(CLIParam.shpt_source_prefix, "/"));
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
}