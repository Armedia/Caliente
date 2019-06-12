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

	private static final double NANOS_PER_SECOND = TimeUnit.SECONDS.toNanos(1);

	public final class ProgressReport {
		private final long startNanoTime;
		private final long triggerNanoTime;
		private final long lastTriggerNanoTime;
		private final long intervalCount;
		private final double intervalRate;
		private final long totalCount;
		private final double totalRate;

		private ProgressReport(long startNanos, long triggerNanos, long lastTriggerNanoTime, long intervalCount,
			double intervalRate, long triggerCount, double triggerRate) {
			this.startNanoTime = startNanos;
			this.triggerNanoTime = triggerNanos;
			this.lastTriggerNanoTime = lastTriggerNanoTime;
			this.intervalCount = intervalCount;
			this.intervalRate = intervalRate;
			this.totalCount = triggerCount;
			this.totalRate = triggerRate;
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

		public double getIntervalRate() {
			return this.intervalRate;
		}

		public long getIntervalNanos() {
			return (this.triggerNanoTime - this.lastTriggerNanoTime);
		}

		public long getTotalCount() {
			return this.totalCount;
		}

		public double getTotalRate() {
			return this.totalRate;
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

			final Long currentCount = this.currentTriggerCount.incrementAndGet();
			final boolean triggerByCount = (forced
				|| ((this.triggerCount > 0) && ((currentCount % this.triggerCount) == 0)));

			// Is it time to show progress? Have 10 seconds passed?
			final long now = System.nanoTime();
			final long last = this.lastTrigger.get();
			final double thisInterval = (now - last);
			final boolean triggerByTime = (forced
				|| ((this.triggerIntervalNanos > 0) && ((now - last) >= this.triggerIntervalNanos)));

			// This avoids a race condition where we don't show successive progress reports from
			// different threads
			final boolean shouldTrigger = (forced || triggerByCount || triggerByTime);
			if (shouldTrigger && this.lastTrigger.compareAndSet(last, now)) {
				final Double prev = this.previousTriggerCount.doubleValue();
				final double totalDuration = now - this.start.get();
				this.previousTriggerCount.set(currentCount.longValue());
				final long countPerInterval = (currentCount.longValue() - prev.longValue());
				final Double intervalRate = ((countPerInterval * ProgressTrigger.NANOS_PER_SECOND) / thisInterval);
				final Double totalRate = ((currentCount.doubleValue() * ProgressTrigger.NANOS_PER_SECOND)
					/ totalDuration);
				this.trigger.accept(new ProgressReport(this.start.get(), now, last, countPerInterval, intervalRate,
					currentCount, totalRate));
			}
		}
	}
}