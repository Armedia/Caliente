package com.delta.cmsmf.launcher;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.mail.MessagingException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import com.armedia.cmf.engine.CmfCrypt;
import com.armedia.cmf.engine.TransferSetting;
import com.armedia.cmf.engine.cmis.CmisSessionSetting;
import com.armedia.cmf.engine.exporter.ExportEngine;
import com.armedia.cmf.engine.exporter.ExportEngineListener;
import com.armedia.cmf.engine.exporter.ExportResult;
import com.armedia.cmf.engine.exporter.ExportSkipReason;
import com.armedia.cmf.engine.exporter.ExportState;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfObjectCounter;
import com.armedia.cmf.storage.CmfType;
import com.armedia.commons.dfc.pool.DfcSessionFactory;
import com.armedia.commons.utilities.PluggableServiceLocator;
import com.armedia.commons.utilities.Tools;
import com.delta.cmsmf.cfg.CLIParam;
import com.delta.cmsmf.cfg.Setting;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.utils.CMSMFUtils;

public class AbstractCMSMFMain_export extends AbstractCMSMFMain<ExportEngineListener, ExportEngine<?, ?, ?, ?, ?, ?>>
	implements ExportEngineListener {

	protected static final String EXPORT_START = "cmsmfExportStart";
	protected static final String EXPORT_END = "cmsmfExportEnd";
	protected static final String BASE_SELECTOR = "cmsmfBaseSelector";
	protected static final String FINAL_SELECTOR = "cmsmfFinalSelector";

	protected final CmfObjectCounter<ExportResult> counter = new CmfObjectCounter<ExportResult>(ExportResult.class);

	protected AbstractCMSMFMain_export(ExportEngine<?, ?, ?, ?, ?, ?> engine) throws Throwable {
		super(engine, true, true);
	}

	protected void validateState() throws CMSMFException {
		if (this.server == null) { throw new CMSMFException(
			"Must provide the location where the repository may be accessed"); }
	}

	protected final void prepareSettings(Map<String, Object> settings) throws CMSMFException {
		settings.put(TransferSetting.EXCLUDE_TYPES.getLabel(), Setting.CMF_EXCLUDE_TYPES.getString(""));
		settings.put(TransferSetting.IGNORE_CONTENT.getLabel(), CLIParam.skip_content.isPresent());
		settings.put(TransferSetting.LATEST_ONLY.getLabel(),
			CLIParam.no_versions.isPresent() || CLIParam.direct_fs.isPresent());
		settings.put(TransferSetting.NO_RENDITIONS.getLabel(),
			CLIParam.no_renditions.isPresent() || CLIParam.direct_fs.isPresent());
		if (this.user != null) {
			settings.put(CmisSessionSetting.USER.getLabel(), this.user);
		}
		if (this.password != null) {
			settings.put(CmisSessionSetting.PASSWORD.getLabel(), this.password);
		}
		if (this.domain != null) {
			settings.put(CmisSessionSetting.DOMAIN.getLabel(), this.domain);
		}
		customizeSettings(settings);
	}

	protected void customizeSettings(Map<String, Object> settings) throws CMSMFException {
	}

	/**
	 * <p>
	 * Loads the stored settings for the given job name. If no settings are stored for the given job
	 * (i.e. no settings file found), then {@code null} is returned. Otherwise, the found settings
	 * (whatever they may be) will be returned.
	 * </p>
	 *
	 * @param jobName
	 * @return the settings stored for the given job name, or {@code null} if there are none
	 * @throws CMSMFException
	 */
	protected Map<String, Object> loadSettings(String jobName) throws CMSMFException {
		return null;
	}

	protected Map<String, Object> loadDefaultSettings() throws CMSMFException {
		return new HashMap<String, Object>();
	}

	protected boolean storeSettings(String jobName, Map<String, Object> settings, Date exportStart, Date exportEnd)
		throws CMSMFException {
		return false;
	}

	protected void processSettings(Map<String, Object> settings, boolean loaded, boolean resetJob)
		throws CMSMFException {
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
		PluggableServiceLocator<ExportEngineListener> extraListeners = new PluggableServiceLocator<ExportEngineListener>(
			ExportEngineListener.class);
		extraListeners.setErrorListener(new PluggableServiceLocator.ErrorListener() {
			@Override
			public void errorRaised(Class<?> serviceClass, Throwable t) {
				AbstractCMSMFMain_export.this.log.warn(String.format(
					"Failed to register an additional listener class [%s]", serviceClass.getCanonicalName()), t);
			}
		});
		extraListeners.setHideErrors(false);
		for (ExportEngineListener l : extraListeners) {
			this.engine.addListener(l);
		}

		final String jobName = CLIParam.job_name.getString();
		final boolean resetJob = CLIParam.reset_job.isPresent();
		validateState();
		Map<String, Object> settings = new HashMap<String, Object>();
		prepareSettings(settings);
		settings.put(TransferSetting.THREAD_COUNT.getLabel(),
			Setting.THREADS.getInt(AbstractCMSMFMain.DEFAULT_THREADS));

		Date end = null;
		Map<CmfType, Integer> summary = null;
		String exceptionReport = null;
		StringBuilder report = new StringBuilder();
		Date start = null;
		try {
			prepareState(settings);

			boolean loaded = false;
			if (!StringUtils.isBlank(jobName)) {
				this.log.info(String.format("##### Loading settings for job [%s] #####", jobName));
				Map<String, Object> m = loadSettings(jobName);
				if (m != null) {
					this.log.info(String.format("##### Loaded settings for job [%s] #####", jobName));
					for (String s : m.keySet()) {
						Object v = m.get(s);
						this.log.info(String.format("\t[%s] = [%s]", s, v));
						settings.put(s, v);
					}
					loaded = true;
				}
			}

			if (!loaded) {
				this.log.info(String.format("##### Loading default settings #####", jobName));
				Map<String, Object> m = loadDefaultSettings();
				if ((m != null) && !m.isEmpty()) {
					settings.putAll(m);
				}
			}

			processSettings(settings, loaded, resetJob);
			// Re-encrypt the password
			String pass = Tools.toString(settings.get(DfcSessionFactory.PASSWORD));
			CmfCrypt crypto = this.engine.getCrypto();
			if (pass != null) {
				pass = crypto.decrypt(pass);
			} else {
				pass = "";
			}
			try {
				pass = crypto.encrypt(pass);
			} catch (Exception e) {
				// Ignore, use literal
			}
			settings.put(DfcSessionFactory.PASSWORD, pass);
			start = new Date();
			try {
				this.log.info("##### Export Process Started #####");
				this.counter.reset();
				this.engine.runExport(this.console, this.cmfObjectStore, this.cmfContentStore, settings);
				final Date exportEnd = new Date();
				this.log.info("##### Export Process Finished #####");

				summary = this.cmfObjectStore.getStoredObjectTypes();
				// First, check to see if anything was actually exported...if not, then there's no
				// need
				// to update the stored settings
				boolean exportEmpty = true;
				for (CmfType t : summary.keySet()) {
					if (summary.get(t) > 0) {
						exportEmpty = false;
						// TODO: Add code to destroy anything created, where applicable, if the
						// export is empty. This avoids the accumulation of garbage
						/*
						this.cmfObjectStore.destroyIfEmpty();
						this.cmfContentStore.destroyIfEmpty();
						*/
						break;
					}
				}
				if (!StringUtils.isBlank(jobName) && (!loaded || !exportEmpty || resetJob)) {
					this.log.info(String.format("##### Storing settings for job [%s] #####", jobName));
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

		report.append(String.format("%n%n%nFull Result Report:%n")).append(StringUtils.repeat("=", 30));
		report.append(String.format("%n%s%n", this.counter.generateFullReport(0)));

		Map<ExportResult, Integer> m = this.counter.getCummulative();
		final Integer zero = Integer.valueOf(0);
		report.append(String.format("Result summary:%n%n")).append(StringUtils.repeat("=", 30));
		for (ExportResult r : ExportResult.values()) {
			Integer i = m.get(r);
			if (i == null) {
				i = zero;
			}
			report.append(String.format("%n%-16s : %8d", r.name(), i.intValue()));
		}

		if (exceptionReport != null) {
			report.append(String.format("%n%n%nEXCEPTION REPORT FOLLOWS:%n%n")).append(exceptionReport);
			this.console
				.error(String.format("%n%nEXCEPTION CAUGHT WHILE RUNNING THE EXPORT:%n%n%s%n", exceptionReport));
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
	public final void exportStarted(ExportState exportState) {
		this.console.info(String.format("Export process started with settings:%n%n\t%s%n%n", exportState.cfg));
	}

	@Override
	public final void objectExportStarted(UUID jobId, CmfType objectType, String objectId) {
		this.console.info(String.format("Object export started for %s[%s]", objectType.name(), objectId));
	}

	@Override
	public final void objectExportCompleted(UUID jobId, CmfObject<?> object, Long objectNumber) {
		if (objectNumber != null) {
			this.console.info(String.format("%s export completed for [%s](%s) as object #%d", object.getType().name(),
				object.getLabel(), object.getId(), objectNumber));
			this.counter.increment(object.getType(), ExportResult.EXPORTED);
		}
	}

	@Override
	public final void objectSkipped(UUID jobId, CmfType objectType, String objectId, ExportSkipReason reason) {
		if (reason == ExportSkipReason.SKIPPED) {
			this.console.info(String.format("%s object [%s] was skipped (%s)", objectType.name(), objectId, reason));
			this.counter.increment(objectType, ExportResult.SKIPPED);
		}
	}

	@Override
	public final void objectExportFailed(UUID jobId, CmfType objectType, String objectId, Throwable thrown) {
		this.counter.increment(objectType, ExportResult.FAILED);
		this.console.warn(String.format("Object export failed for %s[%s]", objectType.name(), objectId), thrown);
	}

	@Override
	public final void exportFinished(ExportState exportState, Map<CmfType, Integer> summary) {
		this.console.info("");
		this.console.info("Export Summary");
		this.console.info("");
		for (CmfType t : CmfType.values()) {
			Integer v = summary.get(t);
			if ((v == null) || (v.intValue() == 0)) {
				continue;
			}
			this.console.info(String.format("%-16s : %8d", t.name(), v.intValue()));
		}
		this.console.info("");
		Map<ExportResult, Integer> m = this.counter.getCummulative();
		final Integer zero = Integer.valueOf(0);
		this.console.info("Result summary:");
		this.console.info("");
		for (ExportResult r : ExportResult.values()) {
			Integer i = m.get(r);
			if (i == null) {
				i = zero;
			}
			this.console.info(String.format("%-16s : %8d", r.name(), i.intValue()));
		}
		this.console.info("");
		this.console.info("Export process finished");
	}
}