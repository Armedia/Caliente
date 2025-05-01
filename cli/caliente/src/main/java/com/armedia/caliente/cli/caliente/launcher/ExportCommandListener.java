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

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;

import com.armedia.caliente.engine.exporter.ExportEngineListener;
import com.armedia.caliente.engine.exporter.ExportResult;
import com.armedia.caliente.engine.exporter.ExportSkipReason;
import com.armedia.caliente.engine.exporter.ExportState;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectCounter;
import com.armedia.caliente.store.CmfObjectSearchSpec;
import com.armedia.commons.utilities.Tools;

public class ExportCommandListener extends AbstractCommandListener implements ExportEngineListener {

	public static final Duration DEFAULT_PROGRESS_INTERVAL = Duration
		.ofSeconds(AbstractCommandListener.PROGRESS_INTERVAL);
	private static final long NANOS_IN_SECOND = TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS);

	protected final Duration progressInterval;
	protected final long progressNanos;
	protected final double progressIntervalSeconds;
	protected final CmfObjectCounter<ExportResult> counter = new CmfObjectCounter<>(ExportResult.class);
	protected final AtomicReference<Instant> start = new AtomicReference<>(null);
	protected final AtomicLong previous = new AtomicLong(0);
	protected final AtomicLong objectCounter = new AtomicLong(0);
	protected final Set<String> sources = new LinkedHashSet<>();

	public ExportCommandListener(Logger console) {
		this(console, null);
	}

	public ExportCommandListener(Logger console, Duration progressInterval) {
		super(console);
		this.progressInterval = Tools.coalesce(progressInterval, ExportCommandListener.DEFAULT_PROGRESS_INTERVAL);
		this.progressNanos = this.progressInterval.toNanos();
		this.progressIntervalSeconds = Long.valueOf(this.progressInterval.getSeconds()).doubleValue();
	}

	public final CmfObjectCounter<ExportResult> getCounter() {
		return this.counter;
	}

	private boolean showProgress() {
		return showProgress(false);
	}

	private boolean showProgress(boolean forced) {
		final long totalItems = this.objectCounter.get();
		final boolean milestone = (forced || ((totalItems > 0) && ((totalItems % 1000) == 0)));

		// Is it time to show progress? Have 10 seconds passed?
		final long now = System.nanoTime();
		final long last = this.progressReporter.get();
		final long sinceLast = (now - last);
		final boolean shouldDisplay = (milestone || ((now - last) >= this.progressNanos));

		// This avoids a race condition where we don't show successive progress reports from
		// different threads
		final boolean shown = (shouldDisplay && this.progressReporter.compareAndSet(last, now));
		if (shown) {
			String objectLine = "";
			final Duration totalDuration = Duration.between(this.start.get(), Instant.now());
			final Duration currentDuration = Duration.ofNanos(sinceLast);
			final long currentItems = (totalItems - this.previous.getAndSet(totalItems));
			final double currentRate = ((double) currentItems / (double) currentDuration.toNanos())
				* ExportCommandListener.NANOS_IN_SECOND;
			final double totalRate = ((double) totalItems / (double) totalDuration.toNanos())
				* ExportCommandListener.NANOS_IN_SECOND;

			objectLine = String.format(
				"Exported %,d messages (~%.2f/s, %,d since last report ~%s ago (%,d ns), ~%.2f/s average)", totalItems,
				currentRate, currentItems, currentDuration, currentDuration.toNanos(), totalRate);
			this.console.info("PROGRESS REPORT{}\t{}{}{}{}", Tools.NL, objectLine, Tools.NL, Tools.NL,
				this.counter.generateCummulativeReport(1));
		}
		return shown;
	}

	@Override
	public void exportStarted(ExportState exportState) {
		this.start.set(Instant.now());
		this.console.info("Export process started with settings:{}{}\t{}{}{}", exportState.cfg);
	}

	@Override
	public void sourceSearchStarted(String source) {
		if (this.sources.add(source)) {
			this.console.info("Started retrieving export targets from [{}]", source);
		}
	}

	@Override
	public void sourceSearchMilestone(String source, long sourceCount, long totalCount) {
		this.console.info("Source [{}] has yielded {} export targets so far ({} total)", source, sourceCount,
			totalCount);
	}

	@Override
	public void sourceSearchCompleted(String source, long sourceCount, long totalCount) {
		this.console.info("Source [{}] yielded {} export targets ({} total)", source, sourceCount, totalCount);
	}

	@Override
	public void sourceSearchFailed(String source, long sourceCount, long totalCount, Exception thrown) {
		this.console.error(
			"The export target retrieval from the source [{}] was stopped by an exception after {} objects ({} total objects retrieved)",
			source, sourceCount, totalCount, thrown);
	}

	@Override
	public void searchCompleted(long totalCount) {
		final int totalSources = this.sources.size();
		this.console.info("Retrieved {} export targets from {} {}source{}", totalCount, totalSources,
			totalSources > 1 ? "different " : "", totalSources > 1 ? "s" : "");
	}

	@Override
	public void searchFailed(long totalCount, Exception thrown) {
		final int totalSources = this.sources.size();
		this.console.error(
			"The export target retrieval was stopped by an exception after {} total export targets were retrieved from {} {}source{}",
			totalCount, totalSources, totalSources > 1 ? "different " : "", totalSources > 1 ? "s" : "", thrown);
	}

	@Override
	public void objectExportStarted(UUID jobId, CmfObjectSearchSpec object, CmfObjectSearchSpec referrent) {
		startPinger(this::showProgress);
		if (referrent == null) {
			this.console.info("Object export started for {}", object.getShortLabel());
		} else {
			this.console.info("Object export started for {} (referenced by {})", object.getShortLabel(),
				referrent.getShortLabel());
		}
	}

	@Override
	public void objectExportCompleted(UUID jobId, CmfObject<?> object, Long objectNumber) {
		this.objectCounter.incrementAndGet();
		if (objectNumber != null) {
			this.console.info("Export completed for {} as object #{}", object.getDescription(), objectNumber);
			this.counter.increment(object.getType(), ExportResult.EXPORTED);
		}
	}

	@Override
	public void objectSkipped(UUID jobId, CmfObjectSearchSpec object, ExportSkipReason reason, String extraInfo) {
		this.objectCounter.incrementAndGet();
		switch (reason) {
			case SKIPPED:
			case UNSUPPORTED:
				this.counter.increment(object.getType(), ExportResult.SKIPPED);
				if (extraInfo != null) {
					this.console.info("{} was skipped ({}: {})", object.getShortLabel(), reason, extraInfo);
				} else {
					this.console.info("{} was skipped ({})", object.getShortLabel(), object.getType().name(),
						object.getId(), reason);
				}
				break;
			default:
				break;
		}
	}

	@Override
	public void objectExportFailed(UUID jobId, CmfObjectSearchSpec object, Throwable thrown) {
		this.objectCounter.incrementAndGet();
		this.counter.increment(object.getType(), ExportResult.FAILED);
		this.console.warn("Object export failed for {}", object.getShortLabel(), thrown);
	}

	@Override
	public void consistencyWarning(UUID jobId, CmfObjectSearchSpec object, String fmt, Object... args) {
		// TODO: Raise a warning
	}

	@Override
	public final void exportFinished(UUID jobId, Map<CmfObject.Archetype, Long> summary) {
		stopPinger();
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