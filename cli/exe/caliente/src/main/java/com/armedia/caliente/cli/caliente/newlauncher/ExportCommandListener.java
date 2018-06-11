package com.armedia.caliente.cli.caliente.newlauncher;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;

import com.armedia.caliente.engine.exporter.ExportEngineListener;
import com.armedia.caliente.engine.exporter.ExportResult;
import com.armedia.caliente.engine.exporter.ExportSkipReason;
import com.armedia.caliente.engine.exporter.ExportState;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectCounter;
import com.armedia.caliente.store.CmfObjectRef;
import com.armedia.caliente.store.CmfType;

public class ExportCommandListener implements ExportEngineListener {

	private static final Integer PROGRESS_INTERVAL = 5;

	private final Logger console;
	private final CalienteWarningTracker warningTracker;
	private final CmfObjectCounter<ExportResult> counter = new CmfObjectCounter<>(ExportResult.class);
	private final AtomicLong start = new AtomicLong(0);
	private final AtomicLong previous = new AtomicLong(0);
	private final AtomicLong progressReporter = new AtomicLong(System.currentTimeMillis());
	private final AtomicLong objectCounter = new AtomicLong();

	public ExportCommandListener(Logger console) {
		this.console = console;
		this.warningTracker = new CalienteWarningTracker(console, true);
	}

	public final CalienteWarningTracker getWarningTracker() {
		return this.warningTracker;
	}

	public final CmfObjectCounter<ExportResult> getCounter() {
		return this.counter;
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
			.convert(ExportCommandListener.PROGRESS_INTERVAL, TimeUnit.SECONDS)));

		// This avoids a race condition where we don't show successive progress reports from
		// different threads
		if (shouldDisplay && this.progressReporter.compareAndSet(last, now)) {
			String objectLine = "";
			final Double prev = this.previous.doubleValue();
			final Long duration = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - this.start.get());
			this.previous.set(current.longValue());
			final long count = (current.longValue() - prev.longValue());
			final Double itemRate = (count / ExportCommandListener.PROGRESS_INTERVAL.doubleValue());
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
		// TODO Auto-generated method stub

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