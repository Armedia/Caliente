package com.armedia.caliente.cli.caliente.newlauncher.dctm;

import java.util.Map;

import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.caliente.command.ImportCommandModule;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.engine.importer.ImportEngine;
import com.armedia.commons.dfc.pool.DfcSessionFactory;

class DctmImporter extends ImportCommandModule {
	DctmImporter(ImportEngine<?, ?, ?, ?, ?, ?> engine) {
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
		if (!DctmEngineInterface.commonConfigure(commandValues, settings)) { return false; }

		String server = null;
		String user = null;
		String password = null;

		if (server != null) {
			settings.put(DfcSessionFactory.DOCBASE, server);
		}
		if (user != null) {
			settings.put(DfcSessionFactory.USERNAME, user);
		}
		if (password != null) {
			settings.put(DfcSessionFactory.PASSWORD, password);
		}

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