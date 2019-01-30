package com.armedia.caliente.cli.caliente.launcher;

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
import com.armedia.caliente.store.CmfObjectSearchSpec;

public class ExportCommandListener extends AbstractCommandListener implements ExportEngineListener {

	protected final CmfObjectCounter<ExportResult> counter = new CmfObjectCounter<>(ExportResult.class);
	protected final AtomicLong start = new AtomicLong(0);
	protected final AtomicLong previous = new AtomicLong(0);
	protected final AtomicLong objectCounter = new AtomicLong();

	public ExportCommandListener(Logger console) {
		super(console);
	}

	public final CmfObjectCounter<ExportResult> getCounter() {
		return this.counter;
	}

	private void showProgress() {
		showProgress(false);
	}

	private void showProgress(boolean forced) {
		final Long current = this.objectCounter.get();
		final boolean milestone = (forced || ((current % 1000) == 0));

		// Is it time to show progress? Have 10 seconds passed?
		long now = System.currentTimeMillis();
		long last = this.progressReporter.get();
		boolean shouldDisplay = (milestone || ((now - last) >= TimeUnit.MILLISECONDS
			.convert(AbstractCommandListener.PROGRESS_INTERVAL, TimeUnit.SECONDS)));

		// This avoids a race condition where we don't show successive progress reports from
		// different threads
		if (shouldDisplay && this.progressReporter.compareAndSet(last, now)) {
			String objectLine = "";
			final Double prev = this.previous.doubleValue();
			final Long duration = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - this.start.get());
			this.previous.set(current.longValue());
			final long count = (current.longValue() - prev.longValue());
			final Double itemRate = (count / AbstractCommandListener.PROGRESS_INTERVAL.doubleValue());
			final Double startRate = (current.doubleValue() / duration.doubleValue());

			objectLine = String.format("Exported %d objects (~%.2f/s, %d since last report, ~%.2f/s average)",
				current.longValue(), itemRate, count, startRate);
			this.console.info(
				String.format("PROGRESS REPORT%n\t%s%n%n%s", objectLine, this.counter.generateCummulativeReport(1)));
		}
	}

	@Override
	public void exportStarted(ExportState exportState) {
		this.start.set(System.currentTimeMillis());
		this.console.info(String.format("Export process started with settings:%n%n\t%s%n%n", exportState.cfg));
	}

	@Override
	public void objectExportStarted(UUID jobId, CmfObjectSearchSpec object, CmfObjectSearchSpec referrent) {
		if (referrent == null) {
			this.console.info(String.format("Object export started for %s", object.getShortLabel()));
		} else {
			this.console.info(String.format("Object export started for %s (referenced by %s)", object.getShortLabel(),
				referrent.getShortLabel()));
		}
	}

	@Override
	public void objectExportCompleted(UUID jobId, CmfObject<?> object, Long objectNumber) {
		this.objectCounter.incrementAndGet();
		if (objectNumber != null) {
			this.console
				.info(String.format("Export completed for %s as object #%d", object.getDescription(), objectNumber));
			this.counter.increment(object.getType(), ExportResult.EXPORTED);
		}
		showProgress();
	}

	@Override
	public void objectSkipped(UUID jobId, CmfObjectSearchSpec object, ExportSkipReason reason, String extraInfo) {
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
	public void objectExportFailed(UUID jobId, CmfObjectSearchSpec object, Throwable thrown) {
		this.objectCounter.incrementAndGet();
		this.counter.increment(object.getType(), ExportResult.FAILED);
		this.console.warn(String.format("Object export failed for %s", object.getShortLabel()), thrown);
		showProgress();
	}

	@Override
	public void consistencyWarning(UUID jobId, CmfObjectSearchSpec object, String fmt, Object... args) {
		// TODO: Raise a warning
	}

	@Override
	public final void exportFinished(UUID jobId, Map<CmfObject.Archetype, Long> summary) {
		showProgress(true);
		this.console.info("");
		this.console.info("Export Summary");
		this.console.info("");
		final String format = "%-16s : %12d";
		for (CmfObject.Archetype t : CmfObject.Archetype.values()) {
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