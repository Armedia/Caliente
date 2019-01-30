package com.armedia.caliente.cli.caliente.launcher;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;

import com.armedia.caliente.engine.importer.ImportEngineListener;
import com.armedia.caliente.engine.importer.ImportOutcome;
import com.armedia.caliente.engine.importer.ImportResult;
import com.armedia.caliente.engine.importer.ImportState;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfArchetype;

public class ImportCommandListener extends AbstractCommandListener implements ImportEngineListener {

	private final AtomicLong aggregateTotal = new AtomicLong(0);
	private final AtomicLong aggregateCurrent = new AtomicLong(0);

	private final Map<CmfArchetype, Long> total = new HashMap<>();
	private final Map<CmfArchetype, AtomicLong> current = new HashMap<>();
	private final Map<CmfArchetype, AtomicLong> previous = new HashMap<>();

	public ImportCommandListener(Logger console) {
		super(console);
	}

	private void showProgress(CmfArchetype objectType) {
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
			.convert(AbstractCommandListener.PROGRESS_INTERVAL, TimeUnit.SECONDS)));

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
				final Double itemRate = (count / AbstractCommandListener.PROGRESS_INTERVAL.doubleValue());

				objectLine = String.format("%n\tProcessed %d/%d %s objects (%.2f%%, ~%.2f/s, %d since last report)",
					current.longValue(), total.longValue(), objectType.name(), itemPct, itemRate, count);
			}
			this.console.info(String.format("PROGRESS REPORT%s%n\tProcessed %d/%d objects in total (%.2f%%)",
				objectLine, aggregateCurrent.longValue(), aggregateTotal.longValue(), aggregatePct));
		}
	}

	@Override
	public final void importStarted(ImportState importState, Map<CmfArchetype, Long> summary) {
		this.aggregateCurrent.set(0);
		this.total.clear();
		this.current.clear();
		this.console.info("Import process started");
		for (CmfArchetype t : CmfArchetype.values()) {
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
	public final void objectTypeImportStarted(UUID jobId, CmfArchetype objectType, long totalObjects) {
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
	public final void objectTypeImportFinished(UUID jobId, CmfArchetype objectType, Map<ImportResult, Long> counters) {
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
	public void objectTierImportStarted(UUID jobId, CmfArchetype objectType, int tier) {
		showProgress(objectType);
	}

	@Override
	public void objectHistoryImportStarted(UUID jobId, CmfArchetype objectType, String historyId, int count) {
		showProgress(objectType);
	}

	@Override
	public void objectHistoryImportFinished(UUID jobId, CmfArchetype objectType, String historyId,
		Map<String, Collection<ImportOutcome>> outcomes, boolean failed) {
		showProgress(objectType);
	}

	@Override
	public void objectTierImportFinished(UUID jobId, CmfArchetype objectType, int tier, boolean failed) {
		showProgress(objectType);
	}
}