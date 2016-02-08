package com.delta.cmsmf.launcher.local;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.armedia.cmf.engine.exporter.ExportEngineListener;
import com.armedia.cmf.engine.local.common.LocalCommon;
import com.armedia.cmf.engine.local.exporter.LocalExportEngine;
import com.armedia.cmf.engine.tools.LocalOrganizationStrategy;
import com.delta.cmsmf.cfg.CLIParam;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.launcher.AbstractCMSMFMain_export;

public class CMSMFMain_export extends AbstractCMSMFMain_export implements ExportEngineListener {

	private File source = null;

	public CMSMFMain_export() throws Throwable {
		super(LocalExportEngine.getExportEngine());
		this.source = new File(CLIParam.source.getString());
	}

	@Override
	protected void validateState() throws CMSMFException {
		// Make sure a source has been specified
		if (this.source == null) { throw new CMSMFException("Must specify a source to export from"); }
		if (!this.source.exists()) { throw new CMSMFException(
			String.format("The specified source at [%s] does not exist", this.source.getPath())); }
		if (!this.source.exists()) { throw new CMSMFException(
			String.format("The specified source at [%s] does not exist", this.source.getPath())); }
		if (!this.source.isDirectory()) { throw new CMSMFException(
			String.format("The specified source at [%s] is not a directory", this.source.getPath())); }
		if (!this.source.canRead()) { throw new CMSMFException(
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
	protected void customizeSettings(Map<String, Object> settings) throws CMSMFException {
		settings.put(LocalCommon.ROOT, this.source.getAbsolutePath());
	}

	@Override
	protected String getContentStrategyName() {
		return LocalOrganizationStrategy.NAME;
	}
}