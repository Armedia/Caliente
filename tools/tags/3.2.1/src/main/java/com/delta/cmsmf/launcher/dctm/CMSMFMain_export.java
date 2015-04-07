package com.delta.cmsmf.launcher.dctm;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.mail.MessagingException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import com.armedia.cmf.engine.TransferSetting;
import com.armedia.cmf.engine.documentum.exporter.DctmExportEngine;
import com.armedia.cmf.engine.exporter.ExportEngine;
import com.armedia.cmf.engine.exporter.ExportEngineListener;
import com.armedia.cmf.engine.exporter.ExportResult;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.commons.dfc.pool.DfcSessionFactory;
import com.armedia.commons.dfc.pool.DfcSessionPool;
import com.armedia.commons.utilities.Tools;
import com.delta.cmsmf.cfg.CLIParam;
import com.delta.cmsmf.cfg.Setting;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.launcher.AbstractCMSMFMain;
import com.delta.cmsmf.launcher.ExportManifest;
import com.delta.cmsmf.utils.CMSMFUtils;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfTime;
import com.documentum.fc.common.IDfTime;

public class CMSMFMain_export extends AbstractCMSMFMain<ExportEngineListener, ExportEngine<?, ?, ?, ?, ?>> implements
ExportEngineListener {

	protected static final String LAST_EXPORT_DATETIME_PATTERN = IDfTime.DF_TIME_PATTERN26;

	/**
	 * The from and where clause of the export query that runs periodically. The application will
	 * combine the select clause listed above with this from and where clauses to build the complete
	 * dql query. Please note that this clause will be ignored when the export is running in the
	 * adhoc mode. In that case the from and where clauses are specified in the properties file.
	 */
	private static final String DEFAULT_PREDICATE = "dm_sysobject where (TYPE(\"dm_folder\") or TYPE(\"dm_document\")) "
		+ "and not folder('/System', descend)"; // and r_modify_date >= DATE('XX_PLACE_HOLDER_XX')";

	public CMSMFMain_export() throws Throwable {
		super(DctmExportEngine.getExportEngine());
	}

	/**
	 * Starts exporting objects from source directory. It reads up the query from properties file
	 * and executes it against the source repository. It retrieves objects from the repository and
	 * exports it out.
	 *
	 */
	@Override
	public void run() throws CMSMFException {
		Set<ExportResult> outcomes = Tools.parseEnumCSV(ExportResult.class, Setting.MANIFEST_OUTCOMES.getString(),
			AbstractCMSMFMain.ALL, false);
		Set<StoredObjectType> types = Tools.parseEnumCSV(StoredObjectType.class, Setting.MANIFEST_TYPES.getString(),
			AbstractCMSMFMain.ALL, false);
		this.engine.addListener(this);
		this.engine.addListener(new ExportManifest(outcomes, types));

		Map<String, Object> settings = new HashMap<String, Object>();
		if (this.server != null) {
			settings.put(DfcSessionFactory.DOCBASE, this.server);
		}
		if (this.user != null) {
			settings.put(DfcSessionFactory.USERNAME, this.user);
		}
		if (this.password != null) {
			settings.put(DfcSessionFactory.PASSWORD, this.password);
		}
		settings.put(TransferSetting.EXCLUDE_TYPES.getLabel(), Setting.CMF_EXCLUDE_TYPES.getString(null));

		final DfcSessionPool pool;
		try {
			pool = new DfcSessionPool(settings);
		} catch (Exception e) {
			throw new CMSMFException("Failed to initialize the connection pool", e);
		}

		Date end = null;
		Map<StoredObjectType, Integer> summary = null;
		String exceptionReport = null;
		StringBuilder report = new StringBuilder();
		Date start = null;
		try {
			final IDfSession session;
			try {
				session = pool.acquireSession();
			} catch (Exception e) {
				throw new CMSMFException("Failed to obtain the main session from the pool", e);
			}

			String dql = String.format("select r_object_id from %s", buildExportPredicate(session));
			settings.put("dql", dql);

			start = new Date();
			try {
				this.log.info("##### Export Process Started #####");
				this.engine.runExport(this.console, this.objectStore, this.contentStore, settings);
				this.log.info("##### Export Process Finished #####");

				/**
				 * Now, we try to set the last export date
				 */

				boolean ok = false;
				try {
					session.beginTrans();
					// If this is auto run type of an export instead of an adhoc query export, store
					// the value of the current export date in the repository. This value will be
					// looked up in the next run. This is indeed an auto run type of export
					DctmUtils.setLastExportDate(session, start);
					ok = true;
				} finally {
					try {
						if (ok) {
							session.commitTrans();
						} else {
							session.abortTrans();
						}
					} catch (DfException e) {
						this.log.error(String.format(
							"Exception caught while %s the transaction for saving the export metadata",
							ok ? "committing" : "aborting"), e);
					}
				}

				summary = this.objectStore.getStoredObjectTypes();
			} catch (Throwable t) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				report.append(String.format("%n%nException caught while attempting an export%n%n"));
				t.printStackTrace(pw);
				exceptionReport = sw.toString();
			} finally {
				// unlock
				end = new Date();
			}

			try {
				session.disconnect();
			} catch (DfException e) {
				if (this.log.isTraceEnabled()) {
					this.log.warn("Exception caught when releasing the master session", e);
				} else {
					this.log.warn("Exception caught when releasing the master session: {}", e.getMessage());
				}
			}
		} finally {
			pool.close();
		}

		long duration = (end.getTime() - start.getTime());
		long hours = TimeUnit.HOURS.convert(duration, TimeUnit.MILLISECONDS);
		duration -= hours * TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS);
		long minutes = TimeUnit.MINUTES.convert(duration, TimeUnit.MILLISECONDS);
		duration -= minutes * TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES);
		long seconds = TimeUnit.SECONDS.convert(duration, TimeUnit.MILLISECONDS);
		report.append(String.format("Export process start    : %s%n",
			DateFormatUtils.format(start, AbstractCMSMFMain.JAVA_SQL_DATETIME_PATTERN)));
		report.append(String.format("Export process end      : %s%n",
			DateFormatUtils.format(end, AbstractCMSMFMain.JAVA_SQL_DATETIME_PATTERN)));
		report.append(String.format("Export process duration : %02d:%02d:%02d%n", hours, minutes, seconds));

		report.append(String.format("%n%nParameters in use:%n")).append(StringUtils.repeat("=", 30));
		for (CLIParam p : CLIParam.values()) {
			String v = p.getString();
			if (v == null) {
				continue;
			}
			report.append(String.format("%n\t--%s = [%s]", p.option.getLongOpt(), v));
		}

		report.append(String.format("%n%n%nSettings in use:%n")).append(StringUtils.repeat("=", 30));
		for (Setting s : Setting.values()) {
			report.append(String.format("%n\t%s = [%s]", s.name, s.getString()));
		}

		if (summary != null) {
			report.append(String.format("%n%n%nExported Object Summary:%n")).append(StringUtils.repeat("=", 30));
			int total = 0;
			for (StoredObjectType t : summary.keySet()) {
				Integer count = summary.get(t);
				if ((count == null) || (count == 0)) {
					continue;
				}
				report.append(String.format("%n%-16s: %6d", t.name(), count));
				total += count;
			}
			report.append(String.format("%n%s%n%-16s: %6d%n", StringUtils.repeat("=", 30), "Total", total));
		}

		if (exceptionReport != null) {
			report.append(String.format("%n%n%nEXCEPTION REPORT FOLLOWS:%n%n")).append(exceptionReport);
		}

		String reportString = report.toString();
		this.log.info(String.format("Action report for export operation:%n%n%s%n", reportString));
		try {
			CMSMFUtils.postCmsmfMail(String.format("Action report for CMSMF Export"), reportString);
		} catch (MessagingException e) {
			this.log.error("Exception caught attempting to send the report e-mail", e);
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
	public boolean requiresCleanData() {
		return true;
	}

	@Override
	public void exportStarted(Map<String, ?> exportSettings) {
		this.console.info(String.format("Export process started with settings:%n%n\t%s%n%n", exportSettings));
	}

	@Override
	public void objectExportStarted(StoredObjectType objectType, String objectId) {
		this.console.info(String.format("Object export started for %s[%s]", objectType.name(), objectId));
	}

	@Override
	public void objectExportCompleted(StoredObject<?> object, Long objectNumber) {
		if (objectNumber != null) {
			this.console.info(String.format("%s export completed for [%s](%s) as object #%d", object.getType().name(),
				object.getLabel(), object.getId(), objectNumber));
		}
	}

	@Override
	public void objectSkipped(StoredObjectType objectType, String objectId) {
		this.console.info(String.format("%s object [%s] was skipped", objectType.name(), objectId));
	}

	@Override
	public void objectExportFailed(StoredObjectType objectType, String objectId, Throwable thrown) {
		this.console.warn(String.format("Object export failed for %s[%s]", objectType.name(), objectId), thrown);
	}

	@Override
	public void exportFinished(Map<StoredObjectType, Integer> summary) {
		this.console.info("Export process finished");
		for (StoredObjectType t : StoredObjectType.values()) {
			Integer v = summary.get(t);
			if ((v == null) || (v.intValue() == 0)) {
				continue;
			}
			this.console.info(String.format("%-16s : %8d", t.name(), v.intValue()));
		}
	}

	@Override
	protected String getContentStrategyName() {
		return "cmsmf";
	}
}