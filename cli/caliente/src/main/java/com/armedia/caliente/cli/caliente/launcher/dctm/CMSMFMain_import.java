package com.armedia.caliente.cli.caliente.launcher.dctm;

import java.util.Map;

import com.armedia.caliente.cli.caliente.exception.CMSMFException;
import com.armedia.caliente.cli.caliente.launcher.AbstractCMSMFMain_import;
import com.armedia.caliente.engine.documentum.importer.DctmImportEngine;
import com.armedia.commons.dfc.pool.DfcSessionFactory;

/**
 * The main method of this class is an entry point for the cmsmf application.
 *
 * @author Shridev Makim 6/15/2010
 */
public class CMSMFMain_import extends AbstractCMSMFMain_import {

	public CMSMFMain_import() throws Throwable {
		super(DctmImportEngine.getImportEngine());
	}

	@Override
	protected void customizeSettings(Map<String, Object> settings) throws CMSMFException {
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