package com.armedia.caliente.cli.caliente.launcher;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
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
import com.armedia.caliente.engine.importer.ImportEngine;
import com.armedia.caliente.engine.importer.ImportEngineListener;
import com.armedia.caliente.engine.importer.ImportOutcome;
import com.armedia.caliente.engine.importer.ImportResult;
import com.armedia.caliente.engine.importer.ImportSetting;
import com.armedia.caliente.engine.importer.ImportState;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectCounter;
import com.armedia.caliente.store.CmfType;
import com.armedia.commons.utilities.PluggableServiceLocator;
import com.armedia.commons.utilities.Tools;

public abstract class AbstractCalienteMain_import extends
	AbstractCalienteModule<ImportEngineListener, ImportEngine<?, ?, ?, ?, ?, ?>> implements ImportEngineListener {

	private static final Integer PROGRESS_INTERVAL = 5;
	private final AtomicLong progressReporter = new AtomicLong(System.currentTimeMillis());
	private final AtomicLong aggregateTotal = new AtomicLong(0);
	private final AtomicLong aggregateCurrent = new AtomicLong(0);

	private final Map<CmfType, Long> total = new HashMap<>();
	private final Map<CmfType, AtomicLong> current = new HashMap<>();
	private final Map<CmfType, AtomicLong> previous = new HashMap<>();

	public AbstractCalienteMain_import(ImportEngine<?, ?, ?, ?, ?, ?> engine) throws Throwable {
		super(engine, true, false, false);
	}

	protected void customizeSettings(Map<String, Object> settings) throws CalienteException {
		settings.put(ImportSetting.NO_FILENAME_MAP.getLabel(), CLIParam.no_filename_map.isPresent());
		settings.put(ImportSetting.FILENAME_MAP.getLabel(), Setting.FILENAME_MAP.getString());
		settings.put(ImportSetting.VALIDATE_REQUIREMENTS.getLabel(), CLIParam.validate_requirements.isPresent());
	}

	@Override
	public final void run() throws CalienteException {
		Set<ImportResult> outcomes = Tools.parseEnumCSV(ImportResult.class, Setting.MANIFEST_OUTCOMES.getString(),
			AbstractCalienteModule.ALL, false);
		Set<CmfType> types = Tools.parseEnumCSV(CmfType.class, Setting.MANIFEST_TYPES.getString(),
			AbstractCalienteModule.ALL, false);
		this.engine.addListener(this);
		this.engine.addListener(new ImportManifest(outcomes, types));
		PluggableServiceLocator<ImportEngineListener> extraListeners = new PluggableServiceLocator<>(
			ImportEngineListener.class);
		extraListeners.setErrorListener(new PluggableServiceLocator.ErrorListener() {
			@Override
			public void errorRaised(Class<?> serviceClass, Throwable t) {
				AbstractCalienteMain_import.this.log.warn(String.format(
					"Failed to register an additional listener class [%s]", serviceClass.getCanonicalName()), t);
			}
		});
		extraListeners.setHideErrors(false);

		for (ImportEngineListener l : extraListeners) {
			this.engine.addListener(l);
		}

		// lock
		Map<String, Object> settings = new HashMap<>();
		settings.put(TransferSetting.EXCLUDE_TYPES.getLabel(), Setting.CMF_EXCLUDE_TYPES.getString(""));
		settings.put(TransferSetting.IGNORE_CONTENT.getLabel(), CLIParam.skip_content.isPresent());
		settings.put(TransferSetting.THREAD_COUNT.getLabel(),
			Setting.THREADS.getInt(AbstractCalienteModule.DEFAULT_THREADS));
		settings.put(ImportSetting.TARGET_LOCATION.getLabel(), Setting.CMF_IMPORT_TARGET_LOCATION.getString("/"));
		settings.put(ImportSetting.TRIM_PREFIX.getLabel(), Setting.CMF_IMPORT_TRIM_PREFIX.getInt(0));
		settings.put(ImportSetting.REPLACE_MODE.getLabel(), Setting.REPLACE_MODE.getString());
		customizeSettings(settings);

		final StringBuilder report = new StringBuilder();
		Date start = new Date();
		Date end = null;
		String exceptionReport = null;
		final CmfObjectCounter<ImportResult> results = new CmfObjectCounter<>(ImportResult.class);
		try {
			this.log.info("##### Import Process Started #####");
			this.engine.runImport(this.console, this.warningTracker, this.cmfObjectStore, this.cmfContentStore,
				settings, results);
			// TODO: run the post-process if necessary
			// importer.doImport(this.sessionManager, Setting.POST_PROCESS_IMPORT.getBoolean());
			this.log.info("##### Import Process Completed #####");
		} catch (Throwable t) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			report.append(String.format("%n%nException caught while attempting an export%n%n"));
			t.printStackTrace(pw);
			exceptionReport = sw.toString();
		} finally {
			end = new Date();
			// unlock
		}

		long duration = (end.getTime() - start.getTime());
		long hours = TimeUnit.HOURS.convert(duration, TimeUnit.MILLISECONDS);
		duration -= hours * TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS);
		long minutes = TimeUnit.MINUTES.convert(duration, TimeUnit.MILLISECONDS);
		duration -= minutes * TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES);
		long seconds = TimeUnit.SECONDS.convert(duration, TimeUnit.MILLISECONDS);
		report.append(String.format("Import process start    : %s%n",
			DateFormatUtils.format(start, AbstractCalienteModule.JAVA_SQL_DATETIME_PATTERN)));
		report.append(String.format("Import process end      : %s%n",
			DateFormatUtils.format(end, AbstractCalienteModule.JAVA_SQL_DATETIME_PATTERN)));
		report.append(String.format("Import process duration : %02d:%02d:%02d%n", hours, minutes, seconds));

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

		report.append(String.format("%n%nAction Summary:%n%s%n", StringUtils.repeat("=", 30)));
		for (CmfType t : CmfType.values()) {
			report.append(String.format("%n%n%n"));
			report.append(results.generateReport(t, 1));
		}
		report.append(String.format("%n%n%n"));
		report.append(results.generateCummulativeReport(1));

		if (exceptionReport != null) {
			report.append(String.format("%n%n%nEXCEPTION REPORT FOLLOWS:%n%n")).append(exceptionReport);
		}

		String reportString = report.toString();
		this.log.info(String.format("Action report for import operation:%n%n%s%n", reportString));
		this.console.info(String.format("Action report for import operation:%n%n%s%n", reportString));
		try {
			EmailUtils.postCalienteMail(String.format("Action report for Caliente Import"), reportString);
		} catch (MessagingException e) {
			this.log.error("Exception caught attempting to send the report e-mail", e);
			this.console.error("Exception caught attempting to send the report e-mail", e);
		}
	}

	private final void showProgress(CmfType objectType) {
		final Double aggregateTotal = this.aggregateTotal.doubleValue();
		final Double aggregateCurrent = this.aggregateCurrent.doubleValue();
		final Double aggregatePct = (aggregateCurrent / aggregateTotal) * 100.0;

		boolean milestone = (aggregateTotal.intValue() == aggregateCurrent.intValue());

		final Long current;
		final Long total;
		if (objectType != null) {
			current = this.current.get(objectType).get();
			total = this.total.get(objectType);
			milestone |= (total.longValue() == current.longValue());
		} else {
			current = null;
			total = null;
		}

		// Is it time to show progress? Have 10 seconds passed?
		long now = System.currentTimeMillis();
		long last = this.progressReporter.get();
		boolean shouldDisplay = (milestone || ((now - last) >= TimeUnit.MILLISECONDS
			.convert(AbstractCalienteMain_import.PROGRESS_INTERVAL, TimeUnit.SECONDS)));

		// This avoids a race condition where we don't show successive progress reports from
		// different threads
		if (shouldDisplay && this.progressReporter.compareAndSet(last, now)) {
			String objectLine = "";
			if (current != null) {
				final AtomicLong itemPrev = this.previous.get(objectType);
				final Double prev = itemPrev.doubleValue();
				itemPrev.set(current);
				final long count = (current.longValue() - prev.longValue());
				final Double itemPct = (current.doubleValue() / total.doubleValue()) * 100.0;
				final Double itemRate = (count / AbstractCalienteMain_import.PROGRESS_INTERVAL.doubleValue());

				objectLine = String.format("%n\tProcessed %d/%d %s objects (%.2f%%, ~%.2f/s, %d since last report)",
					current.longValue(), total.longValue(), objectType.name(), itemPct, itemRate, count);
			}
			this.console.info(String.format("PROGRESS REPORT%s%n\tProcessed %d/%d objects in total (%.2f%%)",
				objectLine, aggregateCurrent.longValue(), aggregateTotal.longValue(), aggregatePct));
		}
	}

	@Override
	public final void importStarted(ImportState importState, Map<CmfType, Long> summary) {
		this.aggregateCurrent.set(0);
		this.total.clear();
		this.current.clear();
		this.console.info("Import process started");
		for (CmfType t : CmfType.values()) {
			Long v = summary.get(t);
			if ((v == null) || (v.longValue() == 0)) {
				continue;
			}
			this.console.info(String.format("%-16s : %8d", t.name(), v.longValue()));
			this.aggregateTotal.addAndGet(v.intValue());
			this.total.put(t, v);
			this.current.put(t, new AtomicLong(0));
			this.previous.put(t, new AtomicLong(0));
		}
	}

	@Override
	public final void objectTypeImportStarted(UUID jobId, CmfType objectType, long totalObjects) {
		showProgress(objectType);
		this.console.info(String.format("Object import started for %d %s objects", totalObjects, objectType.name()));
	}

	@Override
	public final void objectImportStarted(UUID jobId, CmfObject<?> object) {
		showProgress(object.getType());
		this.console.info(String.format("Import started for %s", object.getDescription()));
	}

	@Override
	public final void objectImportCompleted(UUID jobId, CmfObject<?> object, ImportOutcome outcome) {
		this.aggregateCurrent.incrementAndGet();
		this.current.get(object.getType()).incrementAndGet();
		String suffix = null;
		switch (outcome.getResult()) {
			case CREATED:
			case UPDATED:
			case DUPLICATE:
				suffix = String.format(" as [%s](%s)", outcome.getNewLabel(), outcome.getNewId());
				break;
			default:
				suffix = "";
				break;
		}
		this.console.info(String.format("Import completed for %s: %s%s", object.getDescription(),
			outcome.getResult().name(), suffix));
		showProgress(object.getType());
	}

	@Override
	public final void objectImportFailed(UUID jobId, CmfObject<?> object, Throwable thrown) {
		this.aggregateCurrent.incrementAndGet();
		this.current.get(object.getType()).incrementAndGet();
		this.console.info(String.format("Import failed for %s", object.getDescription()), thrown);
		showProgress(object.getType());
	}

	@Override
	public final void objectTypeImportFinished(UUID jobId, CmfType objectType, Map<ImportResult, Long> counters) {
		this.console.info(String.format("Finished importing %s objects", objectType.name()));
		for (ImportResult r : ImportResult.values()) {
			Long v = counters.get(r);
			if ((v == null) || (v.longValue() == 0)) {
				continue;
			}
			this.console.info(String.format("%-10s: %8d", r.name(), v.longValue()));
		}
		showProgress(objectType);
	}

	@Override
	public final void importFinished(UUID jobId, Map<ImportResult, Long> counters) {
		this.console.info("Import process finished");
		for (ImportResult r : ImportResult.values()) {
			Long v = counters.get(r);
			if ((v == null) || (v.longValue() == 0)) {
				continue;
			}
			this.console.info(String.format("%-10s: %8d", r.name(), v.longValue()));
		}
		showProgress(null);
	}

	@Override
	public void objectTierImportStarted(UUID jobId, CmfType objectType, int tier) {
		showProgress(objectType);
	}

	@Override
	public void objectHistoryImportStarted(UUID jobId, CmfType objectType, String historyId, int count) {
		showProgress(objectType);
	}

	@Override
	public void objectHistoryImportFinished(UUID jobId, CmfType objectType, String historyId,
		Map<String, Collection<ImportOutcome>> outcomes, boolean failed) {
		showProgress(objectType);
	}

	@Override
	public void objectTierImportFinished(UUID jobId, CmfType objectType, int tier, boolean failed) {
		showProgress(objectType);
	}
}