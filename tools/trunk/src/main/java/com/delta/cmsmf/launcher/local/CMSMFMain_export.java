package com.delta.cmsmf.launcher.local;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.armedia.cmf.engine.exporter.ExportEngineListener;
import com.armedia.cmf.engine.local.exporter.LocalExportEngine;
import com.armedia.cmf.engine.tools.LocalOrganizationStrategy;
import com.delta.cmsmf.cfg.CLIParam;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.launcher.AbstractCMSMFMain_export;

public class CMSMFMain_export extends AbstractCMSMFMain_export implements ExportEngineListener {

	public CMSMFMain_export() throws Throwable {
		super(LocalExportEngine.getExportEngine());
	}

	@Override
	protected void customizeSettings(Map<String, Object> settings) throws CMSMFException {
		String srcPath = CLIParam.source.getString();
		if (srcPath == null) { throw new CMSMFException("Must provide the source path to export from"); }

		final File root;
		try {
			root = new File(srcPath).getCanonicalFile();
		} catch (IOException e) {
			throw new CMSMFException(String.format("Failed to find the canonical path of [%s]", srcPath), e);
		}

		settings.put("root", root.getAbsolutePath());
	}

	@Override
	protected String getContentStrategyName() {
		return LocalOrganizationStrategy.NAME;
	}
}