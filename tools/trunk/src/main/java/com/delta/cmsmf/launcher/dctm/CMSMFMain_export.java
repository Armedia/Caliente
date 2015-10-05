package com.delta.cmsmf.launcher.dctm;

import java.util.Date;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.armedia.cmf.engine.documentum.DocumentumOrganizationStrategy;
import com.armedia.cmf.engine.documentum.exporter.DctmExportEngine;
import com.armedia.cmf.engine.exporter.ExportEngineListener;
import com.armedia.commons.dfc.pool.DfcSessionFactory;
import com.armedia.commons.dfc.pool.DfcSessionPool;
import com.delta.cmsmf.cfg.Setting;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.launcher.AbstractCMSMFMain_export;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfTime;
import com.documentum.fc.common.IDfTime;

public class CMSMFMain_export extends AbstractCMSMFMain_export implements ExportEngineListener {

	protected static final String LAST_EXPORT_DATETIME_PATTERN = IDfTime.DF_TIME_PATTERN26;

	/**
	 * The from and where clause of the export query that runs periodically. The application will
	 * combine the select clause listed above with this from and where clauses to build the complete
	 * dql query. Please note that this clause will be ignored when the export is running in the
	 * adhoc mode. In that case the from and where clauses are specified in the properties file.
	 */
	private static final String DEFAULT_PREDICATE = "dm_sysobject where (TYPE(\"dm_folder\") or TYPE(\"dm_document\")) "
		+ "and not folder('/System', descend)"; // and r_modify_date >= DATE('XX_PLACE_HOLDER_XX')";

	private DfcSessionPool pool = null;
	private IDfSession session = null;

	public CMSMFMain_export() throws Throwable {
		super(DctmExportEngine.getExportEngine());
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
		String predicate = Setting.EXPORT_PREDICATE.getString();
		if (StringUtils.isBlank(predicate)) {
			// Try to locate a object in source repository that represents a last successful export
			// to a target repository.
			// NOTE : We will create a cabinet named 'CMSMF_SYNC' in source repository. We will
			// create a folder for each target repository in this cabinet, the name of the folder
			// will be the name of a target repository. In this folder we will create an object
			// named 'cmsmf_last_export' and

			// first get the last export date from the source repository
			predicate = CMSMFMain_export.DEFAULT_PREDICATE;
		}
		// Set the default predicate...
		settings.put("dql", String.format("select r_object_id from %s", predicate));
	}

	@Override
	protected void prepareState(Map<String, Object> settings) throws CMSMFException {
		try {
			this.pool = new DfcSessionPool(settings);
			this.session = this.pool.acquireSession();
		} catch (Exception e) {
			throw new CMSMFException("Failed to initialize the connection pool or get the primary session", e);
		}
	}

	@Override
	protected void cleanupState() {
		if (this.session != null) {
			this.pool.releaseSession(this.session);
		}
		if (this.pool != null) {
			this.pool.close();
		}
	}

	@Override
	protected boolean loadSettings(String jobName, Map<String, Object> settings) throws CMSMFException {

		try {
			/**
			 * Now, we try to set the last export date
			 */
			try {
				this.session.beginTrans();
				String dql = String.format("select r_object_id from %s", buildExportPredicate(this.session));
				settings.put("dql", dql);
			} finally {
				try {
					this.session.abortTrans();
				} catch (DfException e) {
					this.log.error(
						"Exception caught while rolling back the transaction for saving the export metadata", e);
				}
			}
			return true;
		} catch (Exception e) {
			throw new CMSMFException("Exception caught storing the export metadata", e);
		}
	}

	@Override
	protected boolean storeSettings(String jobName, Map<String, Object> settings, Date exportStart, Date exportEnd)
		throws CMSMFException {

		try {
			/**
			 * Now, we try to set the last export date
			 */
			boolean ok = false;
			try {
				this.session.beginTrans();
				// If this is auto run type of an export instead of an adhoc query export, store
				// the value of the current export date in the repository. This value will be
				// looked up in the next run. This is indeed an auto run type of export
				DctmUtils.setLastExportDate(this.session, exportStart);
				this.session.commitTrans();
				ok = true;
			} finally {
				try {
					if (!ok) {
						this.session.abortTrans();
					}
				} catch (DfException e) {
					this.log.error(
						"Exception caught while rolling back the transaction for saving the export metadata", e);
				}
			}
			return true;
		} catch (Exception e) {
			throw new CMSMFException("Exception caught storing the export metadata", e);
		}
	}

	@Override
	protected void processSettings(Map<String, Object> settings, boolean loaded) throws CMSMFException {
	}

	private String buildExportPredicate(IDfSession session) {

		// First check to see if ad-hoc query property has any value. If it does have some value in
		// it, use it to build the query string. If this value is blank, look into the source
		// repository to see when was the last export run and pick up the sysobjects modified since
		// then.

		String predicate = Setting.EXPORT_PREDICATE.getString();
		if (StringUtils.isBlank(predicate)) {
			// Try to locate a object in source repository that represents a last successful export
			// to a target repository.
			// NOTE : We will create a cabinet named 'CMSMF_SYNC' in source repository. We will
			// create a folder for each target repository in this cabinet, the name of the folder
			// will be the name of a target repository. In this folder we will create an object
			// named 'cmsmf_last_export' and

			// first get the last export date from the source repository
			Date lastExportRunDate = DctmUtils.getLastExportDate(session);
			predicate = CMSMFMain_export.DEFAULT_PREDICATE;
			if (lastExportRunDate != null) { return String.format("%s AND r_modify_date >= DATE('%s', '%s')",
				predicate, new DfTime(lastExportRunDate).asString(CMSMFMain_export.LAST_EXPORT_DATETIME_PATTERN),
				CMSMFMain_export.LAST_EXPORT_DATETIME_PATTERN); }
		}
		return predicate;
	}

	@Override
	protected String getContentStrategyName() {
		return DocumentumOrganizationStrategy.NAME;
	}
}