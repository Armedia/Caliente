package com.armedia.caliente.tools;

import java.time.Duration;
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
	public void testReset() {
		Assertions.fail("Not yet implemented");
	}

	@Test
	public void testTriggerBoolean() {
		Assertions.fail("Not yet implemented");
	}

	@Test
	public void testTriggerByCount() {
		final AtomicLong start = new AtomicLong(0);
		final Consumer<Long> startTrigger = (s) -> {
			Assertions.assertEquals(0, start.get());
			start.set(s);
		};
		final long triggerCount = 10000;
		final AtomicLong counter = new AtomicLong(0);
		final Consumer<ProgressReport> trigger = (r) -> {
			Assertions.assertEquals(start.get(), r.getStartNanoTime());
			Assertions.assertEquals(counter.get(), r.getTotalCount());
			Assertions.assertEquals(triggerCount, r.getIntervalCount());
			System.out.printf("Progress report (by count): %d/%dns (~%.3f/s) | %d/%dns (~%.3f/s)%n",
				r.getIntervalCount(), r.getIntervalNanos(), r.getIntervalRate(), r.getTotalCount(), r.getTotalNanos(),
				r.getTotalRate());
		};
		final ProgressTrigger progressTrigger = new ProgressTrigger(startTrigger, trigger, triggerCount);
		for (int i = 0; i < 10000000; i++) {
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
			long timeDelta = (r.getTriggerNanoTime() - r.getLastTriggerNanoTime());
			if (timeDelta < intervalNanos) {
				Assertions.fail(String.format("The interval should have been at least %d nanos, but was %d",
					intervalNanos, timeDelta));
			}
			System.out.printf("Progress report (by time): %d/%dns (~%.3f/s) | %d/%dns (~%.3f/s)%n",
				r.getIntervalCount(), r.getIntervalNanos(), r.getIntervalRate(), r.getTotalCount(), r.getTotalNanos(),
				r.getTotalRate());
		};
		final ProgressTrigger progressTrigger = new ProgressTrigger(startTrigger, trigger, interval);
		for (int i = 0; i < 100000000; i++) {
			counter.incrementAndGet();
			progressTrigger.trigger();
		}
	}
}