package com.armedia.caliente.cli.caliente.launcher.local;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.armedia.caliente.cli.caliente.cfg.CLIParam;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.launcher.AbstractCMSMFMain_export;
import com.armedia.caliente.engine.exporter.ExportEngineListener;
import com.armedia.caliente.engine.local.common.LocalSetting;
import com.armedia.caliente.engine.local.exporter.LocalExportEngine;
import com.armedia.caliente.engine.tools.LocalOrganizationStrategy;

public class CMSMFMain_export extends AbstractCMSMFMain_export implements ExportEngineListener {

	private File source = null;

	public CMSMFMain_export() throws Throwable {
		super(LocalExportEngine.getExportEngine());
		this.source = new File(CLIParam.source.getString());
	}

	@Override
	protected void validateState() throws CalienteException {
		// Make sure a source has been specified
		if (this.source == null) { throw new CalienteException("Must specify a source to export from"); }
		if (!this.source.exists()) { throw new CalienteException(
			String.format("The specified source at [%s] does not exist", this.source.getPath())); }
		if (!this.source.exists()) { throw new CalienteException(
			String.format("The specified source at [%s] does not exist", this.source.getPath())); }
		if (!this.source.isDirectory()) { throw new CalienteException(
			String.format("The specified source at [%s] is not a directory", this.source.getPath())); }
		if (!this.source.canRead()) { throw new CalienteException(
			String.format("The specified source at [%s] is not readable", this.source.getPath())); }
		try {
			File f = this.source.getCanonicalFile();
			this.source = f;
		} catch (IOException e) {
			File f = this.source.getAbsoluteFile();
			if (this.log.isTraceEnabled()) {
				this.log.warn(String.format(
					"Failed to find the canonical path for [%s], will settle for the absolute path at [%s]",
					this.source.getPath(), f.getPath()), e);
			} else {
				this.log.warn(String.format(
					"Failed to find the canonical path for [%s], will settle for the absolute path at [%s]",
					this.source.getPath(), f.getPath()));
			}
			this.source = f;
		}
	}

	@Override
	protected void customizeSettings(Map<String, Object> settings) throws CalienteException {
		settings.put(LocalSetting.ROOT.getLabel(), this.source.getAbsolutePath());
		settings.put(LocalSetting.COPY_CONTENT.getLabel(), false);
	}

	@Override
	protected String getContentStrategyName() {
		return LocalOrganizationStrategy.NAME;
	}
}