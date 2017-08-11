package com.armedia.caliente.cli.caliente.launcher.dctm;

import java.util.Map;

import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.launcher.AbstractCalienteMain_import;
import com.armedia.caliente.engine.dfc.importer.DctmImportEngine;
import com.armedia.commons.dfc.pool.DfcSessionFactory;

/**
 * The main method of this class is an entry point for the Caliente application.
 *
 * @author Shridev Makim 6/15/2010
 */
public class Caliente_import extends AbstractCalienteMain_import {

	public Caliente_import() throws Throwable {
		super(DctmImportEngine.getImportEngine());
	}

	@Override
	protected void customizeSettings(Map<String, Object> settings) throws CalienteException {
		if (this.server != null) {
			settings.put(DfcSessionFactory.DOCBASE, this.server);
		}
		if (this.user != null) {
			settings.put(DfcSessionFactory.USERNAME, this.user);
		}
		if (this.password != null) {
			settings.put(DfcSessionFactory.PASSWORD, this.password);
		}

		super.customizeSettings(settings);
	}
}