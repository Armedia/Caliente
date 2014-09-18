package com.delta.cmsmf.launcher;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.mail.MessagingException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import com.delta.cmsmf.cfg.CLIParam;
import com.delta.cmsmf.cfg.Constant;
import com.delta.cmsmf.cfg.Setting;
import com.delta.cmsmf.cms.CmsCounter;
import com.delta.cmsmf.cms.CmsImportResult;
import com.delta.cmsmf.cms.CmsObject;
import com.delta.cmsmf.cms.CmsObjectType;
import com.delta.cmsmf.engine.CmsImportEventListener;
import com.delta.cmsmf.engine.CmsImporter;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.utils.CMSMFUtils;

/**
 * The main method of this class is an entry point for the cmsmf application.
 *
 * @author Shridev Makim 6/15/2010
 */
public class CMSMFMain_import extends AbstractCMSMFMain implements CmsImportEventListener {

	private final AtomicLong progressReporter = new AtomicLong(System.currentTimeMillis());
	private final AtomicInteger aggregateTotal = new AtomicInteger(0);
	private final AtomicInteger aggregateCurrent = new AtomicInteger(0);

	private final Map<CmsObjectType, Integer> total = new HashMap<CmsObjectType, Integer>();
	private final Map<CmsObjectType, AtomicInteger> current = new HashMap<CmsObjectType, AtomicInteger>();

	CMSMFMain_import() throws Throwable {
		super();
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
		// lock
		final CmsImporter importer = new CmsImporter(Setting.THREADS.getInt());
		importer.addListener(this);
		final StringBuilder report = new StringBuilder();
		Date start = new Date();
		Date end = null;
		String exceptionReport = null;
		try {
			this.log.info("##### Import Process Started #####");
			importer.doImport(this.objectStore, this.sessionManager, this.fileSystem,
				Setting.POST_PROCESS_IMPORT.getBoolean());
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
			DateFormatUtils.format(start, Constant.JAVA_SQL_DATETIME_PATTERN)));
		report.append(String.format("Import process end      : %s%n",
			DateFormatUtils.format(end, Constant.JAVA_SQL_DATETIME_PATTERN)));
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
		CmsCounter<CmsImportResult> counter = importer.getCounter();
		for (CmsObjectType t : CmsObjectType.values()) {
			report.append(String.format("%n%n%n"));
			report.append(counter.generateReport(t, 1));
		}
		report.append(String.format("%n%n%n"));
		report.append(counter.generateCummulativeReport(1));

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

	private void showProgress(CmsObjectType objectType) {
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
		if (milestone || ((now - last) >= TimeUnit.MILLISECONDS.convert(5, TimeUnit.SECONDS))) {
			this.console.info(String.format("PROGRESS REPORT%s%n\tProcessed %d/%d objects in total (%.2f%%)",
				objectLine, aggregateCurrent.intValue(), aggregateTotal.intValue(), aggregatePct));
			this.progressReporter.set(now);
		}
	}

	@Override
	public void importStarted(Map<CmsObjectType, Integer> summary) {
		this.aggregateCurrent.set(0);
		this.total.clear();
		this.current.clear();
		this.console.info("Import process started");
		for (CmsObjectType t : CmsObjectType.values()) {
			Integer v = summary.get(t);
			if ((v == null) || (v.intValue() == 0) || t.isSurrogate()) {
				continue;
			}
			this.console.info(String.format("%-16s : %8d", t.name(), v.intValue()));
			this.aggregateTotal.addAndGet(v.intValue());
			this.total.put(t, v);
			this.current.put(t, new AtomicInteger(0));
		}
	}

	@Override
	public void objectTypeImportStarted(CmsObjectType objectType, int totalObjects) {
		showProgress(objectType);
		this.console.info(String.format("Object import started for %d %s objects", totalObjects, objectType.name()));
	}

	@Override
	public void objectImportStarted(CmsObject<?> object) {
		showProgress(object.getType());
		this.console.info(String.format("Import started for [%s](%s)", object.getLabel(), object.getId()));
	}

	@Override
	public void objectImportCompleted(CmsObject<?> object, CmsImportResult cmsImportResult) {
		this.aggregateCurrent.incrementAndGet();
		this.current.get(object.getType()).incrementAndGet();
		this.console.info(String.format("Import completed for [%s](%s): %s", object.getLabel(), object.getId(),
			cmsImportResult.name()));
		showProgress(object.getType());
	}

	@Override
	public void objectImportFailed(CmsObject<?> object, Throwable thrown) {
		this.aggregateCurrent.incrementAndGet();
		this.current.get(object.getType()).incrementAndGet();
		this.console.info(String.format("Import failed for [%s](%s)", object.getLabel(), object.getId()), thrown);
		showProgress(object.getType());
	}

	@Override
	public void objectTypeImportFinished(CmsObjectType objectType, Map<CmsImportResult, Integer> counters) {
		showProgress(objectType);
		this.console.info(String.format("Finished importing %s objects", objectType.name()));
		for (CmsImportResult r : CmsImportResult.values()) {
			Integer v = counters.get(r);
			if ((v == null) || (v.intValue() == 0)) {
				continue;
			}
			this.console.info(String.format("%-10s: %8d", r.name(), v.intValue()));
		}
	}

	@Override
	public void importFinished(Map<CmsImportResult, Integer> counters) {
		this.console.info("Import process finished");
		for (CmsImportResult r : CmsImportResult.values()) {
			Integer v = counters.get(r);
			if ((v == null) || (v.intValue() == 0)) {
				continue;
			}
			this.console.info(String.format("%-10s: %8d", r.name(), v.intValue()));
		}
	}
}