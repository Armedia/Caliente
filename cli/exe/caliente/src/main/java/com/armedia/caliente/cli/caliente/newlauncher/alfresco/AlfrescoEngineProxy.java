package com.armedia.caliente.cli.caliente.newlauncher.alfresco;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.caliente.cfg.CLIParam;
import com.armedia.caliente.cli.caliente.cfg.Setting;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.newlauncher.EngineProxy;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.engine.alfresco.bi.AlfSetting;
import com.armedia.caliente.engine.alfresco.bi.importer.AlfImportEngine;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.importer.ImportEngine;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.Tools;

public class AlfrescoEngineProxy extends EngineProxy {

	private class AlfrescoImporter extends Importer {
		private AlfrescoImporter(ImportEngine<?, ?, ?, ?, ?, ?> engine) {
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
			String target = commandValues.getString(CLIParam.repository);
			if (target == null) {
				target = ".";
			}
			File targetDir = new File(target).getCanonicalFile();
			targetDir.mkdirs();
			if (!targetDir.exists()) { throw new CalienteException(
				String.format("The target directory [%s] does not exist, and could not be created", targetDir)); }
			if (!targetDir.isDirectory()) { throw new CalienteException(
				String.format("A non-directory already exists at the location [%s] - can't continue", targetDir)); }

			settings.put(AlfSetting.ROOT.getLabel(), targetDir.getAbsolutePath());
			settings.put(AlfSetting.DB.getLabel(), Setting.DB_DIRECTORY.getString());
			settings.put(AlfSetting.CONTENT.getLabel(), Setting.CONTENT_DIRECTORY.getString());
			settings.put(AlfSetting.CONTENT_MODEL.getLabel(),
				Tools.joinCSVEscaped(commandValues.getAllStrings(CLIParam.content_model)));
			return super.doConfigure(commandValues, settings);
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

	public AlfrescoEngineProxy() {
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
		return null;
	}

	@Override
	protected ImportEngine<?, ?, ?, ?, ?, ?> getImportEngine() {
		return AlfImportEngine.getImportEngine();
	}

	@Override
	protected Importer newImporter(ImportEngine<?, ?, ?, ?, ?, ?> engine) {
		return new AlfrescoImporter(engine);
	}

	@Override
	public Collection<? extends LaunchClasspathHelper> getClasspathHelpers() {
		return Collections.emptyList();
	}

}