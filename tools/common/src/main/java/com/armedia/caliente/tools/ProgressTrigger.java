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
package com.armedia.caliente.tools;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.concurrent.BaseShareableLockable;
import com.armedia.commons.utilities.concurrent.MutexAutoLock;
import com.armedia.commons.utilities.concurrent.SharedAutoLock;
import com.armedia.commons.utilities.function.LazySupplier;

public class ProgressTrigger extends BaseShareableLockable {

	public static final ZoneId ZULU = ZoneId.of("Z");

	public static ZonedDateTime fromNanos(long nanos) {
		long seconds = TimeUnit.SECONDS.convert(nanos, TimeUnit.NANOSECONDS);
		Instant instant = Instant.ofEpochSecond(seconds,
			nanos - TimeUnit.NANOSECONDS.convert(seconds, TimeUnit.SECONDS));
		return ZonedDateTime.ofInstant(instant, ProgressTrigger.ZULU);
	}

	public static final Consumer<Long> DEFAULT_START_TRIGGER = (l) -> {
	};

	public static final Duration DEFAULT_TRIGGER_INTERVAL = Duration.ofSeconds(5);
	public static final long DEFAULT_TRIGGER_COUNT = 1000;

	public static final class Statistics {
		private final Duration duration;
		private final long count;

		private final LazySupplier<String> string;

		Statistics(Duration duration, long count) {
			this.duration = Objects.requireNonNull(duration, "Must provide an interval");
			this.count = count;
			this.string = new LazySupplier<>(
				() -> String.format("%d/%s (~%.3f/s)", count, duration, getRatePerSecond()));
		}

		public Duration getDuration() {
			return this.duration;
		}

		public long getCount() {
			return this.count;
		}

		public double getRatePerSecond() {
			return getRatePer(TimeUnit.SECONDS);
		}

		public double getRatePer(TimeUnit timeUnit) {
			double nanoMultiplier = TimeUnit.NANOSECONDS.convert(1,
				Objects.requireNonNull(timeUnit, "Must provide a time unit to calculate with"));
			return (nanoMultiplier * this.count) / this.duration.toNanos();
		}

		@Override
		public String toString() {
			return this.string.get();
		}
	}

	public final class ProgressReport {
		private final ZonedDateTime startTime;
		private final ZonedDateTime triggerTime;
		private final ZonedDateTime lastTriggerTime;

		private final Statistics intervalStatistics;
		private final Statistics aggregateStatistics;

		private final LazySupplier<String> string;

		private ProgressReport(long startNanoTime, long triggerNanoTime, long lastTriggerNanoTime, long intervalCount,
			long aggregateCount) {
			this.startTime = ProgressTrigger.fromNanos(startNanoTime);
			this.triggerTime = ProgressTrigger.fromNanos(triggerNanoTime);
			this.lastTriggerTime = ProgressTrigger.fromNanos(lastTriggerNanoTime);
			this.intervalStatistics = new Statistics(Duration.ofNanos(triggerNanoTime - lastTriggerNanoTime),
				intervalCount);
			this.aggregateStatistics = new Statistics(Duration.ofNanos(triggerNanoTime - startNanoTime),
				aggregateCount);
			this.string = new LazySupplier<>(
				() -> String.format("Interval: %s / Aggregate: %s", this.intervalStatistics, this.aggregateStatistics));
		}

		public long getTriggerCount() {
			return ProgressTrigger.this.triggerCount;
		}

		public Duration getTriggerInterval() {
			return ProgressTrigger.this.triggerInterval;
		}

		public ZonedDateTime getStartTime() {
			return this.startTime;
		}

		public ZonedDateTime getTriggerTime() {
			return this.triggerTime;
		}

		public ZonedDateTime getLastTriggerTime() {
			return this.lastTriggerTime;
		}

		public Statistics getIntervalStatistics() {
			return this.intervalStatistics;
		}

		public Statistics getAggregateStatistics() {
			return this.aggregateStatistics;
		}

		@Override
		public String toString() {
			return this.string.get();
		}
	}

	private final Consumer<Long> startTrigger;
	private final Consumer<ProgressReport> trigger;
	private final Duration triggerInterval;
	private final long triggerIntervalNanos;
	private final long triggerCount;

	private final AtomicBoolean paused = new AtomicBoolean(false);
	private final AtomicLong start = new AtomicLong(0);
	private final AtomicLong lastTrigger = new AtomicLong(0);
	private final AtomicLong previousTriggerCount = new AtomicLong(0);
	private final AtomicLong currentTriggerCount = new AtomicLong();

	public ProgressTrigger(Consumer<ProgressReport> trigger) {
		this(null, trigger, ProgressTrigger.DEFAULT_TRIGGER_COUNT, ProgressTrigger.DEFAULT_TRIGGER_INTERVAL);
	}

	public ProgressTrigger(Consumer<ProgressReport> trigger, long triggerCount) {
		this(null, trigger, triggerCount, null);
	}

	public ProgressTrigger(Consumer<ProgressReport> trigger, Duration triggerInterval) {
		this(null, trigger, 0, triggerInterval);
	}

	public ProgressTrigger(Consumer<ProgressReport> trigger, long triggerCount, Duration triggerInterval) {
		this(null, trigger, triggerCount, triggerInterval);
	}

	public ProgressTrigger(Consumer<Long> startTrigger, Consumer<ProgressReport> trigger) {
		this(startTrigger, trigger, ProgressTrigger.DEFAULT_TRIGGER_COUNT, ProgressTrigger.DEFAULT_TRIGGER_INTERVAL);
	}

	public ProgressTrigger(Consumer<Long> startTrigger, Consumer<ProgressReport> trigger, long triggerCount) {
		this(startTrigger, trigger, triggerCount, null);
	}

	public ProgressTrigger(Consumer<Long> startTrigger, Consumer<ProgressReport> trigger, Duration triggerInterval) {
		this(startTrigger, trigger, 0, triggerInterval);
	}

	public ProgressTrigger(Consumer<Long> startTrigger, Consumer<ProgressReport> trigger, long triggerCount,
		Duration triggerInterval) {
		this.trigger = Objects.requireNonNull(trigger, "Must provide a non-null trigger function");
		this.startTrigger = Tools.coalesce(startTrigger, ProgressTrigger.DEFAULT_START_TRIGGER);
		this.triggerInterval = triggerInterval;
		this.triggerIntervalNanos = (triggerInterval != null ? triggerInterval.toNanos() : 0);
		this.triggerCount = Tools.ensureBetween(0L, triggerCount, Long.MAX_VALUE);
		if ((this.triggerIntervalNanos == 0) && (this.triggerCount == 0)) {
			throw new IllegalArgumentException(
				"Must provide a trigger interval of more than 0 nanoseconds, and/or an auto trigger count greater than 0");
		}
	}

	public Duration getTriggerInterval() {
		return this.triggerInterval;
	}

	public long getTriggerCount() {
		return this.triggerCount;
	}

	public Consumer<Long> getStartTrigger() {
		return this.startTrigger;
	}

	public Consumer<ProgressReport> getTrigger() {
		return this.trigger;
	}

	public final boolean setPaused(boolean paused) {
		return shareLocked(() -> this.paused.getAndSet(paused));
	}

	public final boolean isPaused() {
		return shareLocked(() -> this.paused.get());
	}

	public final void reset() {
		mutexLocked(() -> this.start.set(0));
	}

	public final void trigger() {
		trigger(false);
	}

	public final void trigger(boolean forced) {
		try (SharedAutoLock lock = sharedAutoLock()) {
			// If it's paused, then there's nothing to be done...
			if (this.paused.get()) { return; }

			if (this.start.get() == 0) {
				try (MutexAutoLock mutexLock = lock.upgrade()) {
					if (this.start.get() == 0) {
						this.start.set(System.nanoTime());
						this.lastTrigger.set(this.start.get());
						this.previousTriggerCount.set(0);
						this.currentTriggerCount.set(0);
						this.startTrigger.accept(this.start.get());
					}
				}
			}

			// Have the required number of ticks been logged?
			final long totalCount = this.currentTriggerCount.incrementAndGet();
			final boolean triggerByCount = (forced
				|| ((this.triggerCount > 0) && ((totalCount % this.triggerCount) == 0)));

			// Is it time to show progress?
			final long thisTriggerNanoTime = System.nanoTime();
			final long lastTriggerNanoTime = this.lastTrigger.get();
			final boolean triggerByTime = (forced || ((this.triggerIntervalNanos > 0)
				&& ((thisTriggerNanoTime - lastTriggerNanoTime) >= this.triggerIntervalNanos)));

			// This avoids a race condition where we don't show successive progress reports from
			// different threads
			final boolean shouldTrigger = (forced || triggerByCount || triggerByTime);
			if (shouldTrigger && this.lastTrigger.compareAndSet(lastTriggerNanoTime, thisTriggerNanoTime)) {
				final long previousCount = this.previousTriggerCount.getAndSet(totalCount);
				final long intervalCount = (totalCount - previousCount);
				this.trigger.accept(new ProgressReport(this.start.get(), thisTriggerNanoTime, lastTriggerNanoTime,
					intervalCount, totalCount));
			}
		}
	}

	public final ProgressReport getReport() {
		try (SharedAutoLock lock = sharedAutoLock()) {
			final long totalCount = this.currentTriggerCount.get();
			final long thisTriggerNanoTime = System.nanoTime();
			final long lastTriggerNanoTime = this.lastTrigger.get();

			// This avoids a race condition where we don't show successive progress reports from
			// different threads
			final long previousCount = this.previousTriggerCount.get();
			final long intervalCount = (totalCount - previousCount);
			return new ProgressReport(this.start.get(), thisTriggerNanoTime, lastTriggerNanoTime, intervalCount,
				totalCount);
		}
	}
}