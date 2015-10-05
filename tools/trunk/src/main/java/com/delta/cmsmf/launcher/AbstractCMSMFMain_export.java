package com.delta.cmsmf.launcher;

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
import com.armedia.cmf.engine.cmis.CmisSessionSetting;
import com.armedia.cmf.engine.exporter.ExportEngine;
import com.armedia.cmf.engine.exporter.ExportEngineListener;
import com.armedia.cmf.engine.exporter.ExportResult;
import com.armedia.cmf.engine.exporter.ExportSkipReason;
import com.armedia.cmf.engine.sharepoint.ShptSessionFactory;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfType;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;
import com.delta.cmsmf.cfg.CLIParam;
import com.delta.cmsmf.cfg.Setting;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.utils.CMSMFUtils;

public class AbstractCMSMFMain_export extends AbstractCMSMFMain<ExportEngineListener, ExportEngine<?, ?, ?, ?, ?, ?>>
	implements ExportEngineListener {

	protected AbstractCMSMFMain_export(ExportEngine<?, ?, ?, ?, ?, ?> engine) throws Throwable {
		super(engine);
	}

	protected void validateState() throws CMSMFException {
		if (this.server == null) { throw new CMSMFException(
			"Must provide the location where the repository may be accessed"); }
	}

	protected final void prepareSettings(Map<String, Object> settings) throws CMSMFException {
		settings.put(TransferSetting.EXCLUDE_TYPES.getLabel(), Setting.CMF_EXCLUDE_TYPES.getString(""));
		if (this.user != null) {
			settings.put(CmisSessionSetting.USER.getLabel(), this.user);
		}
		if (this.password != null) {
			settings.put(CmisSessionSetting.PASSWORD.getLabel(), this.password);
		}
		if (this.domain != null) {
			settings.put(ShptSessionFactory.DOMAIN, this.domain);
		}
		customizeSettings(settings);
	}

	protected void customizeSettings(Map<String, Object> settings) throws CMSMFException {
	}

	protected boolean loadSettings(String jobName, Map<String, Object> settings) throws CMSMFException {
		return false;
	}

	protected boolean storeSettings(String jobName, Map<String, Object> settings, Date exportStart, Date exportEnd)
		throws CMSMFException {
		return false;
	}

	protected void processSettings(Map<String, Object> settings, boolean loaded) throws CMSMFException {
	}

	protected void prepareState(Map<String, Object> settings) throws CMSMFException {

	}

	protected void cleanupState() {

	}

	/**
	 * Starts exporting objects from source directory. It reads up the query from properties file
	 * and executes it against the source repository. It retrieves objects from the repository and
	 * exports it out.
	 *
	 */
	@Override
	public final void run() throws CMSMFException {
		Set<ExportResult> outcomes = Tools.parseEnumCSV(ExportResult.class, Setting.MANIFEST_OUTCOMES.getString(),
			AbstractCMSMFMain.ALL, false);
		Set<CmfType> types = Tools.parseEnumCSV(CmfType.class, Setting.MANIFEST_TYPES.getString(),
			AbstractCMSMFMain.ALL, false);
		this.engine.addListener(this);
		this.engine.addListener(new ExportManifest(outcomes, types));

		final String jobName = CLIParam.job_name.getString();

		validateState();
		Map<String, Object> settings = new HashMap<String, Object>();
		prepareSettings(settings);

		boolean loaded = false;
		if (!StringUtils.isBlank(jobName)) {
			this.log.info(String.format("##### Loading settings for job [%s] #####", jobName));
			if (loadSettings(jobName, settings)) {
				this.log.info(String.format("##### Settings for job [%s] #####", jobName));
				for (String s : settings.keySet()) {
					this.log.info(String.format("\t[%s] = [%s]", s, settings.get(s)));
				}
				loaded = true;
			} else {
				this.log.info(String.format("##### No settings stored for job [%s] #####", jobName));
			}
		}

		processSettings(settings, loaded);

		Date end = null;
		Map<CmfType, Integer> summary = null;
		String exceptionReport = null;
		StringBuilder report = new StringBuilder();
		Date start = null;
		try {
			prepareState(settings);
			start = new Date();
			try {
				this.log.info("##### Export Process Started #####");
				this.engine.runExport(this.console, this.cmfObjectStore, this.cmfContentStore, settings);
				final Date exportEnd = new Date();
				this.log.info("##### Export Process Finished #####");

				summary = this.cmfObjectStore.getStoredObjectTypes();
				if (!StringUtils.isBlank(jobName)) {
					this.log.info(String.format("##### Storing settings for job [%s] #####"));
					for (String s : settings.keySet()) {
						this.log.info(String.format("\t[%s] = [%s]", s, settings.get(s)));
					}
					if (storeSettings(jobName, settings, start, exportEnd)) {
						this.log.info(String.format("##### Settings for job [%s] stored successfully #####", jobName));
					} else {
						this.log.info(String.format("##### Settings for job [%s] not stored #####", jobName));
					}
				}
			} catch (Throwable t) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				report.append(String.format("%n%nException caught while attempting an export%n%n"));
				t.printStackTrace(pw);
				exceptionReport = sw.toString();
			}
		} finally {
			// unlock
			end = new Date();
			cleanupState();
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
			if (!p.isPresent()) {
				continue;
			}
			String v = p.getString();
			if (v == null) {
				report.append(String.format("%n\t--%s", p.option.getLongOpt()));
			} else {
				report.append(String.format("%n\t--%s = [%s]", p.option.getLongOpt(), v));
			}
		}

		report.append(String.format("%n%n%nSettings in use:%n")).append(StringUtils.repeat("=", 30));
		for (Setting s : Setting.values()) {
			report.append(String.format("%n\t%s = [%s]", s.name, s.getString()));
		}

		if (summary != null) {
			report.append(String.format("%n%n%nExported Object Summary:%n")).append(StringUtils.repeat("=", 30));
			int total = 0;
			for (CmfType t : summary.keySet()) {
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

	@Override
	public final boolean requiresCleanData() {
		return true;
	}

	@Override
	public final void exportStarted(CfgTools config) {
		this.console.info(String.format("Export process started with settings:%n%n\t%s%n%n", config));
	}

	@Override
	public final void objectExportStarted(CmfType objectType, String objectId) {
		this.console.info(String.format("Object export started for %s[%s]", objectType.name(), objectId));
	}

	@Override
	public final void objectExportCompleted(CmfObject<?> object, Long objectNumber) {
		if (objectNumber != null) {
			this.console.info(String.format("%s export completed for [%s](%s) as object #%d", object.getType().name(),
				object.getLabel(), object.getId(), objectNumber));
		}
	}

	@Override
	public final void objectSkipped(CmfType objectType, String objectId, ExportSkipReason reason) {
		if (reason == ExportSkipReason.SKIPPED) {
			this.console.info(String.format("%s object [%s] was skipped (%s)", objectType.name(), objectId, reason));
		}
	}

	@Override
	public final void objectExportFailed(CmfType objectType, String objectId, Throwable thrown) {
		this.console.warn(String.format("Object export failed for %s[%s]", objectType.name(), objectId), thrown);
	}

	@Override
	public final void exportFinished(Map<CmfType, Integer> summary) {
		this.console.info("Export process finished");
		for (CmfType t : CmfType.values()) {
			Integer v = summary.get(t);
			if ((v == null) || (v.intValue() == 0)) {
				continue;
			}
			this.console.info(String.format("%-16s : %8d", t.name(), v.intValue()));
		}
	}
}