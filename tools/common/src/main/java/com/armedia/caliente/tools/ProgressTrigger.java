package com.armedia.caliente.tools;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.concurrent.BaseShareableLockable;
import com.armedia.commons.utilities.concurrent.MutexAutoLock;
import com.armedia.commons.utilities.concurrent.SharedAutoLock;

public class ProgressTrigger extends BaseShareableLockable {

	public static final Consumer<Long> DEFAULT_START_TRIGGER = (l) -> {
	};

	public static final Duration DEFAULT_TRIGGER_INTERVAL = Duration.ofSeconds(5);
	public static final long DEFAULT_TRIGGER_COUNT = 1000;

	public final class ProgressReport {
		private final long startNanoTime;
		private final long triggerNanoTime;
		private final long lastTriggerNanoTime;
		private final long intervalCount;
		private final long totalCount;

		private ProgressReport(long startNanos, long triggerNanos, long lastTriggerNanoTime, long intervalCount,
			long triggerCount) {
			this.startNanoTime = startNanos;
			this.triggerNanoTime = triggerNanos;
			this.lastTriggerNanoTime = lastTriggerNanoTime;
			this.intervalCount = intervalCount;
			this.totalCount = triggerCount;
		}

		public long getAutoTriggerCount() {
			return ProgressTrigger.this.triggerCount;
		}

		public long getExpectedIntervalNanos() {
			return ProgressTrigger.this.triggerIntervalNanos;
		}

		public long getStartNanoTime() {
			return this.startNanoTime;
		}

		public long getTriggerNanoTime() {
			return this.triggerNanoTime;
		}

		public long getLastTriggerNanoTime() {
			return this.lastTriggerNanoTime;
		}

		public long getIntervalCount() {
			return this.intervalCount;
		}

		private double getRate(double count, long timeNanos, TimeUnit timeUnit) {
			long nanoMultiplier = TimeUnit.NANOSECONDS.convert(1,
				Objects.requireNonNull(timeUnit, "Must provide a time unit to calculate with"));
			double ratePerNanos = (count / timeNanos);
			return ratePerNanos * nanoMultiplier;
		}

		public double getIntervalRate() {
			return getIntervalRate(TimeUnit.SECONDS);
		}

		public double getIntervalRate(TimeUnit timeUnit) {
			return getRate(this.intervalCount, (this.triggerNanoTime - this.lastTriggerNanoTime), timeUnit);
		}

		public long getIntervalNanos() {
			return (this.triggerNanoTime - this.lastTriggerNanoTime);
		}

		public long getTotalCount() {
			return this.totalCount;
		}

		public double getTotalRate() {
			return getTotalRate(TimeUnit.SECONDS);
		}

		public double getTotalRate(TimeUnit timeUnit) {
			return getRate(this.totalCount, (this.triggerNanoTime - this.startNanoTime), timeUnit);
		}

		public long getTotalNanos() {
			return (this.triggerNanoTime - this.startNanoTime);
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
		try (SharedAutoLock lock = autoSharedLock()) {
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

			final Long totalCount = this.currentTriggerCount.incrementAndGet();
			final boolean triggerByCount = (forced
				|| ((this.triggerCount > 0) && ((totalCount % this.triggerCount) == 0)));

			// Is it time to show progress? Have 10 seconds passed?
			final long thisTriggerNanoTime = System.nanoTime();
			final long lastTriggerNanoTime = this.lastTrigger.get();
			final boolean triggerByTime = (forced || ((this.triggerIntervalNanos > 0)
				&& ((thisTriggerNanoTime - lastTriggerNanoTime) >= this.triggerIntervalNanos)));

			// This avoids a race condition where we don't show successive progress reports from
			// different threads
			final boolean shouldTrigger = (forced || triggerByCount || triggerByTime);
			if (shouldTrigger && this.lastTrigger.compareAndSet(lastTriggerNanoTime, thisTriggerNanoTime)) {
				final Double previousCount = this.previousTriggerCount.doubleValue();
				this.previousTriggerCount.set(totalCount.longValue());
				final long intervalCount = (totalCount.longValue() - previousCount.longValue());
				this.trigger.accept(new ProgressReport(this.start.get(), thisTriggerNanoTime, lastTriggerNanoTime,
					intervalCount, totalCount));
			}
		}
	}
}