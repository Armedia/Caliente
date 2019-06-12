package com.armedia.caliente.tools;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.tools.ProgressTrigger.ProgressReport;

public class ProgressTriggerTest {

	@Test
	public void testConstructors() {
	}

	@Test
	public void testGetTriggerInterval() {
		Consumer<ProgressReport> consumer = (r) -> {
		};
		ProgressTrigger trigger = new ProgressTrigger(consumer);

		// It must explode b/c the interval is 0
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> new ProgressTrigger(consumer, Duration.ofMillis(0)));

		// Assert this is the default interval
		Assertions.assertEquals(ProgressTrigger.DEFAULT_TRIGGER_INTERVAL, trigger.getTriggerInterval());

		for (long i = 1; i < 100; i++) {
			Duration interval = Duration.ofMillis(500 * i);
			trigger = new ProgressTrigger(consumer, interval);
			Assertions.assertEquals(interval, trigger.getTriggerInterval());
		}
	}

	@Test
	public void testGetTriggerCount() {
		Consumer<ProgressReport> consumer = (r) -> {
		};
		ProgressTrigger trigger = new ProgressTrigger(consumer);

		// It must explode b/c the interval is 0
		Assertions.assertThrows(IllegalArgumentException.class, () -> new ProgressTrigger(consumer, 0));

		// Assert this is the default interval
		Assertions.assertEquals(ProgressTrigger.DEFAULT_TRIGGER_COUNT, trigger.getTriggerCount());

		for (long i = 1; i < 100; i++) {
			long count = 500 * i;
			trigger = new ProgressTrigger(consumer, count);
			Assertions.assertEquals(count, trigger.getTriggerCount());
		}
	}

	@Test
	public void testGetStartTrigger() {
		Consumer<ProgressReport> consumer = (r) -> {
		};
		ProgressTrigger trigger = new ProgressTrigger(consumer);
		Assertions.assertSame(ProgressTrigger.DEFAULT_START_TRIGGER, trigger.getStartTrigger());

		for (int i = 0; i < 10; i++) {
			final int I = i;
			Consumer<Long> st = (s) -> {
				if (I == s) {
					"".hashCode();
				}
			};
			trigger = new ProgressTrigger(st, consumer);
			Assertions.assertSame(st, trigger.getStartTrigger());
		}
	}

	@Test
	public void testGetTrigger() {
		Consumer<ProgressReport> consumer = (r) -> {
		};
		ProgressTrigger trigger = new ProgressTrigger(consumer);
		Assertions.assertSame(consumer, trigger.getTrigger());

		for (int i = 0; i < 10; i++) {
			final int I = i;
			consumer = (r) -> {
				if (I == r.hashCode()) {
					"".hashCode();
				}
			};
			trigger = new ProgressTrigger(consumer);
			Assertions.assertSame(consumer, trigger.getTrigger());
		}
	}

	@Test
	public void testTriggerByCount() {
		final AtomicLong start = new AtomicLong(0);
		final Consumer<Long> startTrigger = (s) -> {
			Assertions.assertEquals(0, start.get());
			start.set(s);
		};
		final long triggerCount = 1000000;
		final AtomicLong counter = new AtomicLong(0);
		final Consumer<ProgressReport> trigger = (r) -> {
			Assertions.assertEquals(start.get(), r.getStartNanoTime());
			Assertions.assertEquals(counter.get(), r.getTotalCount());
			Assertions.assertEquals(triggerCount, r.getIntervalCount());
			Assertions.assertEquals(triggerCount, r.getTriggerCount());
			System.out.printf("Progress report (by count): %d/%s (~%.3f/s) | %d/%s (~%.3f/s)%n", r.getIntervalCount(),
				r.getIntervalDuration(), r.getIntervalRatePerSecond(), r.getTotalCount(), r.getTotalDuration(),
				r.getTotalRatePerSecond());
		};
		final ProgressTrigger progressTrigger = new ProgressTrigger(startTrigger, trigger, triggerCount);
		for (int i = 0; i < 100000000; i++) {
			counter.incrementAndGet();
			progressTrigger.trigger();
		}
	}

	@Test
	public void testTriggerByTime() {
		final AtomicLong start = new AtomicLong(0);
		final Consumer<Long> startTrigger = (s) -> {
			Assertions.assertEquals(0, start.get());
			start.set(s);
		};
		final Duration interval = Duration.ofMillis(2000);
		final long intervalNanos = interval.toNanos();
		final AtomicLong counter = new AtomicLong(0);
		final Consumer<ProgressReport> trigger = (r) -> {
			Assertions.assertEquals(start.get(), r.getStartNanoTime());
			Assertions.assertEquals(interval, r.getTriggerInterval());
			long timeDelta = (r.getTriggerNanoTime() - r.getLastTriggerNanoTime());
			if (timeDelta < intervalNanos) {
				Assertions.fail(String.format("The interval should have been at least %d nanos, but was %d",
					intervalNanos, timeDelta));
			}
			System.out.printf("Progress report (by time): %d/%s (~%.3f/s) | %d/%s (~%.3f/s)%n", r.getIntervalCount(),
				r.getIntervalDuration(), r.getIntervalRatePerSecond(), r.getTotalCount(), r.getTotalDuration(),
				r.getTotalRatePerSecond());
		};
		final ProgressTrigger progressTrigger = new ProgressTrigger(startTrigger, trigger, interval);
		for (int i = 0; i < 1000000000; i++) {
			counter.incrementAndGet();
			progressTrigger.trigger();
		}
	}

	@Test
	public void testTriggerForced() {
		final AtomicLong start = new AtomicLong(0);
		final Consumer<Long> startTrigger = (s) -> {
			Assertions.assertEquals(0, start.get());
			start.set(s);
		};
		final long triggerCount = 1000000;
		final AtomicLong counter = new AtomicLong(0);
		final Consumer<ProgressReport> trigger = (r) -> {
			Assertions.assertEquals(start.get(), r.getStartNanoTime());
			Assertions.assertEquals(counter.get(), r.getTotalCount());
			Assertions.assertEquals(1, r.getIntervalCount());
		};
		final ProgressTrigger progressTrigger = new ProgressTrigger(startTrigger, trigger, triggerCount);
		for (int i = 0; i < 100000000; i++) {
			counter.incrementAndGet();
			progressTrigger.trigger(true);
		}
	}

	@Test
	public void testReset() {
		final AtomicLong start = new AtomicLong(0);
		final AtomicInteger startCount = new AtomicInteger(0);
		final long triggerCount = 10000;
		final AtomicLong counter = new AtomicLong(0);
		final Consumer<Long> startTrigger = (s) -> {
			startCount.incrementAndGet();
			start.set(s);
		};
		final Consumer<ProgressReport> trigger = (r) -> {
			Assertions.assertEquals(counter.get(), r.getTotalCount());
		};
		final ProgressTrigger progressTrigger = new ProgressTrigger(startTrigger, trigger, triggerCount);
		Assertions.assertEquals(0, startCount.get());
		for (int i = 0; i < 1000000; i++) {
			counter.incrementAndGet();
			progressTrigger.trigger();
		}
		Assertions.assertEquals(1, startCount.get());
		progressTrigger.reset();
		counter.set(0);
		for (int i = 0; i < 1000000; i++) {
			counter.incrementAndGet();
			progressTrigger.trigger();
		}
		Assertions.assertEquals(2, startCount.get());
	}

	@Test
	public void testPause() {
		final AtomicLong start = new AtomicLong(0);
		final AtomicInteger startCount = new AtomicInteger(0);
		final long triggerCount = 10000;
		final AtomicLong counter = new AtomicLong(0);
		final AtomicBoolean paused = new AtomicBoolean(false);
		final Consumer<Long> startTrigger = (s) -> {
			Assertions.assertFalse(paused.get());
			startCount.incrementAndGet();
			start.set(s);
		};
		final Consumer<ProgressReport> trigger = (r) -> {
			Assertions.assertFalse(paused.get());
			Assertions.assertEquals(counter.get(), r.getTotalCount());
		};
		final ProgressTrigger progressTrigger = new ProgressTrigger(startTrigger, trigger, triggerCount);
		Assertions.assertEquals(0, startCount.get());
		Assertions.assertFalse(progressTrigger.isPaused());
		paused.set(progressTrigger.isPaused());
		for (int i = 0; i < 1000000; i++) {
			counter.incrementAndGet();
			progressTrigger.trigger();
		}
		Assertions.assertEquals(1, startCount.get());
		progressTrigger.setPaused(true);
		Assertions.assertTrue(progressTrigger.isPaused());
		paused.set(progressTrigger.isPaused());
		for (int i = 0; i < 1000000; i++) {
			progressTrigger.trigger();
		}
		Assertions.assertEquals(1, startCount.get());
		progressTrigger.setPaused(false);
		Assertions.assertFalse(progressTrigger.isPaused());
		paused.set(progressTrigger.isPaused());
		for (int i = 0; i < 1000000; i++) {
			counter.incrementAndGet();
			progressTrigger.trigger();
		}
		Assertions.assertEquals(1, startCount.get());
	}
}