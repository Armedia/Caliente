package com.delta.cmsmf.launcher.dctm;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.chemistry.opencmis.commons.impl.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import com.armedia.cmf.engine.documentum.DfUtils;
import com.armedia.cmf.engine.documentum.DocumentumOrganizationStrategy;
import com.armedia.cmf.engine.documentum.exporter.DctmExportEngine;
import com.armedia.cmf.engine.exporter.ExportEngineListener;
import com.armedia.commons.dfc.pool.DfcSessionFactory;
import com.armedia.commons.dfc.pool.DfcSessionPool;
import com.delta.cmsmf.cfg.Setting;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.launcher.AbstractCMSMFMain_export;
import com.documentum.fc.client.IDfDocument;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfTime;
import com.documentum.fc.common.IDfTime;

public class CMSMFMain_export extends AbstractCMSMFMain_export implements ExportEngineListener {

	protected static final String LAST_EXPORT_DATETIME_PATTERN = IDfTime.DF_TIME_PATTERN26;

	private static final FastDateFormat DATE_FORMAT = DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT;

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

	private String getJobQualification(String jobName, IDfFolder exportFolder) throws DfException {
		final String fileName = String.format("%s.job", jobName.toLowerCase());
		return String.format("dm_document where object_name = %s and folder(ID(%s))", DfUtils.quoteString(fileName),
			exportFolder.getObjectId().getId());
	}

	@Override
	protected Map<String, Object> loadSettings(String jobName) throws CMSMFException {

		try {
			try {
				this.session.beginTrans();

				IDfFolder exportFolder = DctmUtils.getCmsmfStateCabinet(this.session, false);
				if (exportFolder == null) { return null; }

				final String qualification = getJobQualification(jobName, exportFolder);
				final IDfDocument doc = IDfDocument.class.cast(this.session.getObjectByQualification(qualification));
				Map<String, Object> settings = null;
				if (doc != null) {
					settings = new HashMap<String, Object>();
					InputStream in = doc.getContent();
					Properties props = new Properties();
					try {
						props.loadFromXML(in);
					} finally {
						IOUtils.closeQuietly(in);
					}
					for (Object o : props.keySet()) {
						settings.put(o.toString(), props.get(o));
					}
				}
				return settings;
			} finally {
				try {
					this.session.abortTrans();
				} catch (DfException e) {
					this.log
						.error(
							String
								.format(
									"Exception caught while rolling back the transaction for loading the export metadata for job [%s]",
									jobName), e);
				}
			}
		} catch (Exception e) {
			throw new CMSMFException(
				String.format("Exception caught loading the export settings for job [%s]", jobName), e);
		}
	}

	@Override
	protected Map<String, Object> loadDefaultSettings(String jobName) throws CMSMFException {
		Map<String, Object> settings = super.loadDefaultSettings(jobName);
		String dql = String.format("select r_object_id from %s", buildExportPredicate(this.session));
		settings.put(AbstractCMSMFMain_export.FINAL_SELECTOR, dql);
		return settings;
	}

	@Override
	protected boolean storeSettings(String jobName, Map<String, Object> settings, Date exportStart, Date exportEnd)
		throws CMSMFException {

		try {
			/**
			 * Now, we try to set the last export date
			 */
			boolean ok = false;
			this.session.beginTrans();
			try {
				final IDfFolder exportFolder = DctmUtils.getCmsmfStateCabinet(this.session, true);
				final String qualification = getJobQualification(jobName, exportFolder);
				IDfDocument doc = IDfDocument.class.cast(this.session.getObjectByQualification(qualification));
				Map<String, Object> m = new HashMap<String, Object>();
				if ((settings != null) && !settings.isEmpty()) {
					m.putAll(settings);
				}

				// TODO: Convert to a standard UTC string
				Calendar c = Calendar.getInstance();
				c.setTimeZone(TimeZone.getTimeZone("UTC"));
				c.setTime(exportStart);
				m.put(AbstractCMSMFMain_export.EXPORT_START, CMSMFMain_export.DATE_FORMAT.format(c));
				c.setTime(exportEnd);
				m.put(AbstractCMSMFMain_export.EXPORT_END, CMSMFMain_export.DATE_FORMAT.format(c));

				Properties p = new Properties();
				for (String s : m.keySet()) {
					p.put(s, m.get(s));
				}

				ByteArrayOutputStream out = new ByteArrayOutputStream();
				p.storeToXML(out, String.format("Configurations for sync job [%s]", jobName), "UTF-8");
				IOUtils.closeQuietly(out);

				if (doc != null) {
					doc.checkout();
					doc.setContent(out);
					doc.checkin(false, null);
				} else {
					// Create a new one & save
					doc = IDfDocument.class.cast(this.session.newObject("dm_document"));
					doc.setObjectName(String.format("%s.job", jobName));
					doc.setContent(out);
					doc.link(exportFolder.getObjectId().getId());
					doc.save();
				}

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
		if (loaded) {
			// If there are previous settings, we need to look at BASE_SELECTOR and the dates given,
			// and based on that construct the new FINAL_SELECTOR (dql)
			Object startDate = settings.get(AbstractCMSMFMain_export.EXPORT_START);
			if (startDate != null) {
				final Date d;
				try {
					d = CMSMFMain_export.DATE_FORMAT.parse(startDate.toString());
				} catch (ParseException e) {
					throw new CMSMFException(String.format("Invalid date setting [%s] = [%s]",
						AbstractCMSMFMain_export.EXPORT_START, startDate), e);
				}
				IDfTime t = new DfTime(d);
				Object basePred = settings.get(AbstractCMSMFMain_export.BASE_SELECTOR);
				final String dql = String.format("%s AND r_modify_date >= DATE(%s, %s)", basePred,
					DfUtils.quoteString(t.asString(CMSMFMain_export.LAST_EXPORT_DATETIME_PATTERN)),
					DfUtils.quoteString(CMSMFMain_export.LAST_EXPORT_DATETIME_PATTERN));
				settings.put(AbstractCMSMFMain_export.FINAL_SELECTOR, dql);
			} else {
				settings.put(AbstractCMSMFMain_export.FINAL_SELECTOR,
					settings.get(AbstractCMSMFMain_export.BASE_SELECTOR));
			}
		}
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