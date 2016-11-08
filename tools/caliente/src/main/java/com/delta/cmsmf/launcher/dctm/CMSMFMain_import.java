package com.delta.cmsmf.launcher.dctm;

import java.util.Map;

import com.armedia.caliente.engine.documentum.importer.DctmImportEngine;
import com.armedia.commons.dfc.pool.DfcSessionFactory;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.launcher.AbstractCMSMFMain_import;

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