package com.armedia.caliente.cli.caliente.launcher;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.mail.MessagingException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import com.armedia.caliente.cli.caliente.cfg.CLIParam;
import com.armedia.caliente.cli.caliente.cfg.Setting;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.utils.EmailUtils;
import com.armedia.caliente.engine.TransferSetting;
import com.armedia.caliente.engine.cmis.CmisSessionSetting;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.exporter.ExportEngineListener;
import com.armedia.caliente.engine.exporter.ExportResult;
import com.armedia.caliente.engine.exporter.ExportSkipReason;
import com.armedia.caliente.engine.exporter.ExportState;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectCounter;
import com.armedia.caliente.store.CmfObjectRef;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.dfc.pool.DfcSessionFactory;
import com.armedia.commons.utilities.PluggableServiceLocator;
import com.armedia.commons.utilities.Tools;

public class AbstractCalienteModule_export extends
	AbstractCalienteModule<ExportEngineListener, ExportEngine<?, ?, ?, ?, ?, ?>> implements ExportEngineListener {

	private static final Integer PROGRESS_INTERVAL = 5;

	protected static final String EXPORT_START = "calienteExportStart";
	protected static final String EXPORT_END = "calienteExportEnd";
	protected static final String BASE_SELECTOR = "calienteBaseSelector";
	protected static final String FINAL_SELECTOR = "calienteFinalSelector";

	protected final CmfObjectCounter<ExportResult> counter = new CmfObjectCounter<>(ExportResult.class);

	private final AtomicLong start = new AtomicLong(0);
	private final AtomicLong previous = new AtomicLong(0);
	private final AtomicLong progressReporter = new AtomicLong(System.currentTimeMillis());
	private final AtomicLong objectCounter = new AtomicLong();

	protected AbstractCalienteModule_export(ExportEngine<?, ?, ?, ?, ?, ?> engine) throws Throwable {
		super(engine, true, true, true);
	}

	protected AbstractCalienteModule_export(ExportEngine<?, ?, ?, ?, ?, ?> engine, boolean cleanMetadata,
		boolean cleanStorage) throws Throwable {
		super(engine, true, cleanMetadata, cleanStorage);
	}

	protected void validateState() throws CalienteException {
		if (this.server == null) { throw new CalienteException(
			"Must provide the location where the repository may be accessed"); }
	}

	protected final void prepareSettings(Map<String, Object> settings) throws CalienteException {
		settings.put(TransferSetting.EXCLUDE_TYPES.getLabel(), Setting.CMF_EXCLUDE_TYPES.getString(""));
		settings.put(TransferSetting.IGNORE_CONTENT.getLabel(), CLIParam.skip_content.isPresent());
		settings.put(TransferSetting.LATEST_ONLY.getLabel(),
			CLIParam.no_versions.isPresent() || CLIParam.direct_fs.isPresent());
		settings.put(TransferSetting.NO_RENDITIONS.getLabel(),
			CLIParam.no_renditions.isPresent() || CLIParam.direct_fs.isPresent());
		settings.put(TransferSetting.TRANSFORMATION.getLabel(), CLIParam.transformations.getString());
		settings.put(TransferSetting.FILTER.getLabel(), CLIParam.filters.getString());
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

	protected void customizeSettings(Map<String, Object> settings) throws CalienteException {
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
	 * @throws CalienteException
	 */
	protected Map<String, Object> loadSettings(String jobName) throws CalienteException {
		return null;
	}

	protected Map<String, Object> loadDefaultSettings() throws CalienteException {
		return new HashMap<>();
	}

	protected boolean storeSettings(String jobName, Map<String, Object> settings, Date exportStart, Date exportEnd)
		throws CalienteException {
		return false;
	}

	protected void processSettings(Map<String, Object> settings, boolean loaded, boolean resetJob)
		throws CalienteException {
	}

	protected void prepareState(Map<String, Object> settings) throws CalienteException {

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
	public final void run() throws CalienteException {
		Set<ExportResult> outcomes = Tools.parseEnumCSV(ExportResult.class, Setting.MANIFEST_OUTCOMES.getString(),
			AbstractCalienteModule.ALL, false);
		Set<CmfType> types = Tools.parseEnumCSV(CmfType.class, Setting.MANIFEST_TYPES.getString(),
			AbstractCalienteModule.ALL, false);
		this.engine.addListener(this);
		this.engine.addListener(new ExportManifest(outcomes, types));
		PluggableServiceLocator<ExportEngineListener> extraListeners = new PluggableServiceLocator<>(
			ExportEngineListener.class);
		extraListeners.setErrorListener(new PluggableServiceLocator.ErrorListener() {
			@Override
			public void errorRaised(Class<?> serviceClass, Throwable t) {
				AbstractCalienteModule_export.this.log.warn(String.format(
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
		Map<String, Object> settings = new HashMap<>();
		prepareSettings(settings);
		settings.put(TransferSetting.THREAD_COUNT.getLabel(),
			Setting.THREADS.getInt(AbstractCalienteModule.DEFAULT_THREADS));

		Date end = null;
		Map<CmfType, Long> summary = null;
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
				this.objectCounter.set(0);
				this.engine.runExport(this.console, this.warningTracker, this.cmfObjectStore, this.cmfContentStore,
					settings);
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
			DateFormatUtils.format(start, AbstractCalienteModule.JAVA_SQL_DATETIME_PATTERN)));
		report.append(String.format("Export process end      : %s%n",
			DateFormatUtils.format(end, AbstractCalienteModule.JAVA_SQL_DATETIME_PATTERN)));
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
			long total = 0;
			for (CmfType t : summary.keySet()) {
				Long count = summary.get(t);
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
		if (this.warningTracker.hasWarnings()) {
			report.append(String.format("%n%s%n", this.warningTracker.generateReport()));
		}

		Map<ExportResult, Long> m = this.counter.getCummulative();
		final Long zero = Long.valueOf(0);
		report.append(String.format("Result summary:%n%n")).append(StringUtils.repeat("=", 30));
		for (ExportResult r : ExportResult.values()) {
			Long i = m.get(r);
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
			EmailUtils.postCalienteMail(String.format("Action report for Caliente Export"), reportString);
		} catch (MessagingException e) {
			this.log.error("Exception caught attempting to send the report e-mail", e);
		}
	}

	@Override
	public final void exportStarted(ExportState exportState) {
		this.start.set(System.currentTimeMillis());
		this.console.info(String.format("Export process started with settings:%n%n\t%s%n%n", exportState.cfg));
	}

	protected final void showProgress() {
		showProgress(false);
	}

	protected final void showProgress(boolean forced) {
		final Long current = this.objectCounter.get();
		final boolean milestone = (forced || ((current % 1000) == 0));

		// Is it time to show progress? Have 10 seconds passed?
		long now = System.currentTimeMillis();
		long last = this.progressReporter.get();
		boolean shouldDisplay = (milestone || ((now - last) >= TimeUnit.MILLISECONDS
			.convert(AbstractCalienteModule_export.PROGRESS_INTERVAL, TimeUnit.SECONDS)));

		// This avoids a race condition where we don't show successive progress reports from
		// different threads
		if (shouldDisplay && this.progressReporter.compareAndSet(last, now)) {
			String objectLine = "";
			final Double prev = this.previous.doubleValue();
			final Long duration = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - this.start.get());
			this.previous.set(current.longValue());
			final long count = (current.longValue() - prev.longValue());
			final Double itemRate = (count / AbstractCalienteModule_export.PROGRESS_INTERVAL.doubleValue());
			final Double startRate = (current.doubleValue() / duration.doubleValue());

			objectLine = String.format("Exported %d objects (~%.2f/s, %d since last report, ~%.2f/s average)",
				current.longValue(), itemRate, count, startRate);
			this.console.info(
				String.format("PROGRESS REPORT%n\t%s%n%n%s", objectLine, this.counter.generateCummulativeReport(1)));
		}
	}

	@Override
	public final void objectExportStarted(UUID jobId, CmfObjectRef object, CmfObjectRef referrent) {
		if (referrent == null) {
			this.console.info(String.format("Object export started for %s", object.getShortLabel()));
		} else {
			this.console.info(String.format("Object export started for %s (referenced by %s)", object.getShortLabel(),
				referrent.getShortLabel()));
		}
	}

	@Override
	public final void objectExportCompleted(UUID jobId, CmfObject<?> object, Long objectNumber) {
		this.objectCounter.incrementAndGet();
		if (objectNumber != null) {
			this.console
				.info(String.format("Export completed for %s as object #%d", object.getDescription(), objectNumber));
			this.counter.increment(object.getType(), ExportResult.EXPORTED);
		}
		showProgress();
	}

	@Override
	public final void objectSkipped(UUID jobId, CmfObjectRef object, ExportSkipReason reason, String extraInfo) {
		this.objectCounter.incrementAndGet();
		switch (reason) {
			case SKIPPED:
			case UNSUPPORTED:
			case DEPENDENCY_FAILED:
				this.counter.increment(object.getType(), ExportResult.SKIPPED);
				if (extraInfo != null) {
					this.console
						.info(String.format("%s was skipped (%s: %s)", object.getShortLabel(), reason, extraInfo));
				} else {
					this.console.info(String.format("%s was skipped (%s)", object.getShortLabel(),
						object.getType().name(), object.getId(), reason));
				}
				break;
			default:
				break;
		}
		showProgress();
	}

	@Override
	public final void objectExportFailed(UUID jobId, CmfObjectRef object, Throwable thrown) {
		this.objectCounter.incrementAndGet();
		this.counter.increment(object.getType(), ExportResult.FAILED);
		this.console.warn(String.format("Object export failed for %s", object.getShortLabel()), thrown);
		showProgress();
	}

	@Override
	public void consistencyWarning(UUID jobId, CmfObjectRef object, String fmt, Object... args) {
		// TODO: Track the warning so it can be reported at the end of the export process
	}

	@Override
	public final void exportFinished(UUID jobId, Map<CmfType, Long> summary) {
		showProgress(true);
		this.console.info("");
		this.console.info("Export Summary");
		this.console.info("");
		final String format = "%-16s : %12d";
		for (CmfType t : CmfType.values()) {
			Long v = summary.get(t);
			if ((v == null) || (v.longValue() == 0)) {
				continue;
			}
			this.console.info(String.format(format, t.name(), v.longValue()));
		}
		this.console.info("");
		Map<ExportResult, Long> m = this.counter.getCummulative();
		final Long zero = Long.valueOf(0);
		this.console.info("Result summary:");
		this.console.info("");
		for (ExportResult r : ExportResult.values()) {
			Long i = m.get(r);
			if (i == null) {
				i = zero;
			}
			this.console.info(String.format(format, r.name(), i.longValue()));
		}
		this.console.info("");
		if (this.warningTracker.hasWarnings()) {
			this.warningTracker.generateReport(this.console);
		}
		this.console.info("Export process finished");
	}
}