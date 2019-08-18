/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
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
import com.armedia.commons.utilities.LazyFormatter;
import com.armedia.commons.utilities.Tools;

public class ImportCommandListener extends AbstractCommandListener implements ImportEngineListener {

	private final AtomicLong aggregateTotal = new AtomicLong(0);
	private final AtomicLong aggregateCurrent = new AtomicLong(0);

	private final Map<CmfObject.Archetype, Long> total = new HashMap<>();
	private final Map<CmfObject.Archetype, AtomicLong> current = new HashMap<>();
	private final Map<CmfObject.Archetype, AtomicLong> previous = new HashMap<>();

	public ImportCommandListener(Logger console) {
		super(console);
	}

	private void showProgress(CmfObject.Archetype objectType) {
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
			this.console.info("PROGRESS REPORT{}{}\tProcessed {}/{} objects in total ({}%)", objectLine, Tools.NL,
				aggregateCurrent.longValue(), aggregateTotal.longValue(), LazyFormatter.of("%.2f", aggregatePct));
		}
	}

	@Override
	public final void importStarted(ImportState importState, Map<CmfObject.Archetype, Long> summary) {
		this.aggregateCurrent.set(0);
		this.total.clear();
		this.current.clear();
		this.console.info("Import process started");
		for (CmfObject.Archetype t : CmfObject.Archetype.values()) {
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
	public final void objectTypeImportStarted(UUID jobId, CmfObject.Archetype objectType, long totalObjects) {
		showProgress(objectType);
		this.console.info("Object import started for {} {} objects", totalObjects, objectType.name());
	}

	@Override
	public final void objectImportStarted(UUID jobId, CmfObject<?> object) {
		showProgress(object.getType());
		this.console.info("Import started for {}", object.getDescription());
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
		this.console.info("Import completed for {}: {}{}", object.getDescription(), outcome.getResult().name(), suffix);
		showProgress(object.getType());
	}

	@Override
	public final void objectImportFailed(UUID jobId, CmfObject<?> object, Throwable thrown) {
		this.aggregateCurrent.incrementAndGet();
		this.current.get(object.getType()).incrementAndGet();
		this.console.info("Import failed for {}", object.getDescription(), thrown);
		showProgress(object.getType());
	}

	@Override
	public final void objectTypeImportFinished(UUID jobId, CmfObject.Archetype objectType,
		Map<ImportResult, Long> counters) {
		this.console.info("Finished importing {} objects", objectType.name());
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
	public void objectTierImportStarted(UUID jobId, CmfObject.Archetype objectType, int tier) {
		showProgress(objectType);
	}

	@Override
	public void objectHistoryImportStarted(UUID jobId, CmfObject.Archetype objectType, String historyId, int count) {
		showProgress(objectType);
	}

	@Override
	public void objectHistoryImportFinished(UUID jobId, CmfObject.Archetype objectType, String historyId,
		Map<String, Collection<ImportOutcome>> outcomes, boolean failed) {
		showProgress(objectType);
	}

	@Override
	public void objectTierImportFinished(UUID jobId, CmfObject.Archetype objectType, int tier, boolean failed) {
		showProgress(objectType);
	}
}