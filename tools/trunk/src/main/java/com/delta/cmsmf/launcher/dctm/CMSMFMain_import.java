package com.delta.cmsmf.launcher.dctm;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.mail.MessagingException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import com.armedia.cmf.engine.TransferSetting;
import com.armedia.cmf.engine.documentum.importer.DctmImportEngine;
import com.armedia.cmf.engine.importer.ImportEngine;
import com.armedia.cmf.engine.importer.ImportEngineListener;
import com.armedia.cmf.engine.importer.ImportOutcome;
import com.armedia.cmf.engine.importer.ImportResult;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectCounter;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.commons.dfc.pool.DfcSessionFactory;
import com.armedia.commons.utilities.Tools;
import com.delta.cmsmf.cfg.CLIParam;
import com.delta.cmsmf.cfg.Setting;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.launcher.AbstractCMSMFMain;
import com.delta.cmsmf.launcher.ImportManifest;
import com.delta.cmsmf.utils.CMSMFUtils;

/**
 * The main method of this class is an entry point for the cmsmf application.
 *
 * @author Shridev Makim 6/15/2010
 */
public class CMSMFMain_import extends AbstractCMSMFMain<ImportEngineListener, ImportEngine<?, ?, ?, ?, ?>> implements
ImportEngineListener {

	private final AtomicLong progressReporter = new AtomicLong(System.currentTimeMillis());
	private final AtomicInteger aggregateTotal = new AtomicInteger(0);
	private final AtomicInteger aggregateCurrent = new AtomicInteger(0);

	private final Map<StoredObjectType, Integer> total = new HashMap<StoredObjectType, Integer>();
	private final Map<StoredObjectType, AtomicInteger> current = new HashMap<StoredObjectType, AtomicInteger>();

	public CMSMFMain_import() throws Throwable {
		super(DctmImportEngine.getImportEngine());
	}

	/**
	 * Starts exporting objects from source directory. It reads up the query from properties file
	 * and executes it against the source repository. It retrieves objects from the repository and
	 * exports it out.
	 *
	 * @throws CMSMFException
	 */
	@Override
	public void run() throws CMSMFException {
		Set<ImportResult> outcomes = Tools.parseEnumCSV(ImportResult.class, Setting.MANIFEST_OUTCOMES.getString(),
			AbstractCMSMFMain.ALL, false);
		Set<StoredObjectType> types = Tools.parseEnumCSV(StoredObjectType.class, Setting.MANIFEST_TYPES.getString(),
			AbstractCMSMFMain.ALL, false);
		this.engine.addListener(this);
		this.engine.addListener(new ImportManifest(outcomes, types));

		// lock
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

		final StringBuilder report = new StringBuilder();
		Date start = new Date();
		Date end = null;
		String exceptionReport = null;
		final StoredObjectCounter<ImportResult> results = new StoredObjectCounter<ImportResult>(ImportResult.class);
		try {
			this.log.info("##### Import Process Started #####");
			this.engine.runImport(this.console, this.objectStore, this.contentStore, settings, results);
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
			DateFormatUtils.format(start, AbstractCMSMFMain.JAVA_SQL_DATETIME_PATTERN)));
		report.append(String.format("Import process end      : %s%n",
			DateFormatUtils.format(end, AbstractCMSMFMain.JAVA_SQL_DATETIME_PATTERN)));
		report.append(String.format("Import process duration : %02d:%02d:%02d%n", hours, minutes, seconds));

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

		report.append(String.format("%n%nAction Summary:%n%s%n", StringUtils.repeat("=", 30)));
		for (StoredObjectType t : StoredObjectType.values()) {
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
			CMSMFUtils.postCmsmfMail(String.format("Action report for CMSMF Import"), reportString);
		} catch (MessagingException e) {
			this.log.error("Exception caught attempting to send the report e-mail", e);
			this.console.error("Exception caught attempting to send the report e-mail", e);
		}
	}

	private void showProgress(StoredObjectType objectType) {
		final Double aggregateTotal = this.aggregateTotal.doubleValue();
		final Double aggregateCurrent = this.aggregateCurrent.doubleValue();
		final Double aggregatePct = (aggregateCurrent / aggregateTotal) * 100.0;

		boolean milestone = (aggregateTotal.intValue() == aggregateCurrent.intValue());

		String objectLine = "";
		if (objectType != null) {
			final Double itemTotal = this.total.get(objectType).doubleValue();
			final Double itemCurrent = this.current.get(objectType).doubleValue();
			final Double itemPct = (itemCurrent / itemTotal) * 100.0;
			objectLine = String.format("%n\tProcessed %d/%d %s objects (%.2f%%)", itemCurrent.intValue(),
				itemTotal.intValue(), objectType.name(), itemPct);
			milestone |= (itemTotal.intValue() == itemCurrent.intValue());
		}

		// Is it time to show progress? Have 10 seconds passed?
		long now = System.currentTimeMillis();
		long last = this.progressReporter.get();
		boolean shouldDisplay = (milestone || ((now - last) >= TimeUnit.MILLISECONDS.convert(5, TimeUnit.SECONDS)));
		// This avoids a race condition where we don't show successive progress reports from
		// different threads
		if (shouldDisplay && this.progressReporter.compareAndSet(last, now)) {
			this.console.info(String.format("PROGRESS REPORT%s%n\tProcessed %d/%d objects in total (%.2f%%)",
				objectLine, aggregateCurrent.intValue(), aggregateTotal.intValue(), aggregatePct));
		}
	}

	@Override
	public void importStarted(Map<StoredObjectType, Integer> summary) {
		this.aggregateCurrent.set(0);
		this.total.clear();
		this.current.clear();
		this.console.info("Import process started");
		for (StoredObjectType t : StoredObjectType.values()) {
			Integer v = summary.get(t);
			if ((v == null) || (v.intValue() == 0)) {
				continue;
			}
			this.console.info(String.format("%-16s : %8d", t.name(), v.intValue()));
			this.aggregateTotal.addAndGet(v.intValue());
			this.total.put(t, v);
			this.current.put(t, new AtomicInteger(0));
		}
	}

	@Override
	public void objectTypeImportStarted(StoredObjectType objectType, int totalObjects) {
		showProgress(objectType);
		this.console.info(String.format("Object import started for %d %s objects", totalObjects, objectType.name()));
	}

	@Override
	public void objectImportStarted(StoredObject<?> object) {
		showProgress(object.getType());
		this.console.info(String.format("Import started for %s [%s](%s)", object.getType().name(), object.getLabel(),
			object.getId()));
	}

	@Override
	public void objectImportCompleted(StoredObject<?> object, ImportOutcome outcome) {
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
		this.console.info(String.format("Import completed for %s [%s](%s): %s%s", object.getType().name(),
			object.getLabel(), object.getId(), outcome.getResult().name(), suffix));
		showProgress(object.getType());
	}

	@Override
	public void objectImportFailed(StoredObject<?> object, Throwable thrown) {
		this.aggregateCurrent.incrementAndGet();
		this.current.get(object.getType()).incrementAndGet();
		this.console.info(
			String.format("Import failed for %s [%s](%s)", object.getType().name(), object.getLabel(), object.getId()),
			thrown);
		showProgress(object.getType());
	}

	@Override
	public void objectTypeImportFinished(StoredObjectType objectType, Map<ImportResult, Integer> counters) {
		this.console.info(String.format("Finished importing %s objects", objectType.name()));
		for (ImportResult r : ImportResult.values()) {
			Integer v = counters.get(r);
			if ((v == null) || (v.intValue() == 0)) {
				continue;
			}
			this.console.info(String.format("%-10s: %8d", r.name(), v.intValue()));
		}
		showProgress(objectType);
	}

	@Override
	public void importFinished(Map<ImportResult, Integer> counters) {
		this.console.info("Import process finished");
		for (ImportResult r : ImportResult.values()) {
			Integer v = counters.get(r);
			if ((v == null) || (v.intValue() == 0)) {
				continue;
			}
			this.console.info(String.format("%-10s: %8d", r.name(), v.intValue()));
		}
		showProgress(null);
	}

	@Override
	public void objectBatchImportStarted(StoredObjectType objectType, String batchId, int count) {
		showProgress(objectType);
	}

	@Override
	public void objectBatchImportFinished(StoredObjectType objectType, String batchId,
		Map<String, ImportOutcome> outcomes, boolean failed) {
		showProgress(objectType);
	}
}