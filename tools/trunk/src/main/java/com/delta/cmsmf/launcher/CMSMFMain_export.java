package com.delta.cmsmf.launcher;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.mail.MessagingException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import com.delta.cmsmf.cfg.CLIParam;
import com.delta.cmsmf.cfg.Constant;
import com.delta.cmsmf.cfg.Setting;
import com.delta.cmsmf.cms.CmsObject;
import com.delta.cmsmf.cms.CmsObjectType;
import com.delta.cmsmf.engine.CmsExportEventListener;
import com.delta.cmsmf.engine.CmsExporter;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.utils.CMSMFUtils;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfTime;

/**
 * The main method of this class is an entry point for the cmsmf application.
 *
 * @author Shridev Makim 6/15/2010
 */
public class CMSMFMain_export extends AbstractCMSMFMain implements CmsExportEventListener {

	CMSMFMain_export() throws Throwable {
		super();
	}

	/**
	 * Starts exporting objects from source directory. It reads up the query from properties file
	 * and executes it against the source repository. It retrieves objects from the repository and
	 * exports it out.
	 *
	 */
	@Override
	public void run() throws CMSMFException {

		CmsExporter exporter = new CmsExporter(this.console, Setting.THREADS.getInt());
		exporter.addListener(this);

		final Date start = new Date();
		Date end = null;
		StringBuilder report = new StringBuilder();
		Map<CmsObjectType, Integer> summary = null;
		String exceptionReport = null;
		// lock
		try {
			this.log.info("##### Export Process Started #####");
			exporter.doExport(this.objectStore, this.sessionManager, this.fileSystem, buildExportQueryString());
			this.log.info("##### Export Process Finished #####");

			final IDfSession session = this.sessionManager.acquireSession();
			boolean ok = false;
			try {
				session.beginTrans();
				// If this is auto run type of an export instead of an adhoc query export, store the
				// value of the current export date in the repository. This value will be looked up
				// in the next run. This is indeed an auto run type of export
				CMSMFUtils.setLastExportDate(session, start);
				ok = true;
			} finally {
				try {
					if (ok) {
						session.commitTrans();
					} else {
						session.abortTrans();
					}
				} catch (DfException e) {
					this.log.fatal(String.format(
						"Exception caught while %s the transaction for saving the export metadata", ok ? "committing"
							: "aborting"), e);
				}
				this.sessionManager.releaseSession(session);
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

		long duration = (end.getTime() - start.getTime());
		long hours = TimeUnit.HOURS.convert(duration, TimeUnit.MILLISECONDS);
		duration -= hours * TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS);
		long minutes = TimeUnit.MINUTES.convert(duration, TimeUnit.MILLISECONDS);
		duration -= minutes * TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES);
		long seconds = TimeUnit.SECONDS.convert(duration, TimeUnit.MILLISECONDS);
		report.append(String.format("Export process start    : %s%n",
			DateFormatUtils.format(start, Constant.JAVA_SQL_DATETIME_PATTERN)));
		report.append(String.format("Export process end      : %s%n",
			DateFormatUtils.format(end, Constant.JAVA_SQL_DATETIME_PATTERN)));
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
			for (CmsObjectType t : summary.keySet()) {
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

	private String buildExportQueryString() {

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
			Date lastExportRunDate = null;
			final IDfSession session = this.sessionManager.acquireSession();
			try {
				lastExportRunDate = CMSMFUtils.getLastExportDate(session);
			} finally {
				this.sessionManager.releaseSession(session);
			}
			predicate = Constant.DEFAULT_PREDICATE;
			if (lastExportRunDate != null) { return String.format("%s AND r_modify_date >= DATE('%s', '%s')",
				predicate, new DfTime(lastExportRunDate).asString(Constant.LAST_EXPORT_DATETIME_PATTERN),
				Constant.LAST_EXPORT_DATETIME_PATTERN); }
		}
		return predicate;
	}

	@Override
	public boolean requiresCleanData() {
		return true;
	}

	@Override
	public void exportStarted(String dql) {
		this.console.info(String.format("Export process started with DQL:%n%n\t%s%n%n", dql));
	}

	@Override
	public void objectExportStarted(CmsObjectType objectType, String objectId) {
		this.console.info(String.format("Object export started for %s[%s]", objectType.name(), objectId));
	}

	@Override
	public void objectExportCompleted(CmsObject<?> object) {
		this.console.info(String.format("%s export completed for [%s](%s)", object.getType().name(), object.getLabel(),
			object.getId()));
	}

	@Override
	public void objectSkipped(CmsObjectType objectType, String objectId) {
		this.console.info(String.format("%s object [%s] was skipped", objectType.name(), objectId));
	}

	@Override
	public void objectExportFailed(CmsObjectType objectType, String objectId, Throwable thrown) {
		this.console.warn(String.format("Object export failed for %s[%s]", objectType.name(), objectId), thrown);
	}

	@Override
	public void exportFinished(Map<CmsObjectType, Integer> summary) {
		this.console.info("Export process finished");
		for (CmsObjectType t : CmsObjectType.values()) {
			Integer v = summary.get(t);
			if ((v == null) || (v.intValue() == 0)) {
				continue;
			}
			this.console.info(String.format("%-16s : %8d", t.name(), v.intValue()));
		}
	}
}