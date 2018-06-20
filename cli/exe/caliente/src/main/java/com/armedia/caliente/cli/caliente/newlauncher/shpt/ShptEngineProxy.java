package com.armedia.caliente.cli.caliente.newlauncher.shpt;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.caliente.cfg.CLIParam;
import com.armedia.caliente.cli.caliente.cfg.Setting;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.newlauncher.EngineInterface;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.importer.ImportEngine;
import com.armedia.caliente.engine.sharepoint.ShptSetting;
import com.armedia.caliente.engine.sharepoint.exporter.ShptExportEngine;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.FileNameTools;

public class ShptEngineProxy extends EngineInterface {

	private class ShptExporter extends Exporter {
		private ShptExporter(ExportEngine<?, ?, ?, ?, ?, ?> engine) {
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

			String srcPath = commandValues.getString(CLIParam.source);
			if (srcPath == null) { throw new CalienteException(
				"Must provide the title of the sharepoint site to export"); }
			List<String> l = FileNameTools.tokenize(srcPath, '/');
			if (l.isEmpty()) { throw new CalienteException("Must provide the title of the sharepoint site to export"); }
			final String site = l.get(0);
			if (StringUtils.isEmpty(
				site)) { throw new CalienteException("Must provide the title of the sharepoint site to export"); }

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
				settings.put(ShptSetting.URL.getLabel(), new URL(baseUrl,
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
		protected void postConfigure(OptionValues commandValues, Map<String, Object> settings)
			throws CalienteException {
			super.postConfigure(commandValues, settings);
		}

		@Override
		protected void postValidateSettings(Map<String, Object> settings) throws CalienteException {
			super.postValidateSettings(settings);
		}
	}

	public ShptEngineProxy() {
	}

	@Override
	public String getName() {
		return "alfrescobi";
	}

	@Override
	public Set<String> getAliases() {
		return Collections.emptySet();
	}

	@Override
	public CmfCrypt getCrypt() {
		return new CmfCrypt();
	}

	@Override
	protected ExportEngine<?, ?, ?, ?, ?, ?> getExportEngine() {
		return ShptExportEngine.getExportEngine();
	}

	@Override
	protected Exporter newExporter(ExportEngine<?, ?, ?, ?, ?, ?> engine) {
		return new ShptExporter(engine);
	}

	@Override
	protected ImportEngine<?, ?, ?, ?, ?, ?> getImportEngine() {
		return null;
	}

	@Override
	public Collection<? extends LaunchClasspathHelper> getClasspathHelpers() {
		return Collections.emptyList();
	}

}