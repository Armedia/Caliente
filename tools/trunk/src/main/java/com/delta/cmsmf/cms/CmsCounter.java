package com.delta.cmsmf.cms;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class CmsCounter {

	private static final String NEW_LINE = String.format("%n");

	public static enum Result {
		//
		READ,
		SKIPPED,
		CREATED,
		UPDATED,
		FAILED;
	}

	private static final ReadWriteLock lock = new ReentrantReadWriteLock();
	private static final Map<CmsObjectType, Map<Result, AtomicInteger>> COUNTERS;
	private static final Map<Result, AtomicInteger> CUMMULATIVE;

	static {
		Map<CmsObjectType, Map<Result, AtomicInteger>> counters = new EnumMap<CmsObjectType, Map<Result, AtomicInteger>>(
			CmsObjectType.class);
		for (CmsObjectType objectType : CmsObjectType.values()) {
			Map<Result, AtomicInteger> results = new EnumMap<Result, AtomicInteger>(Result.class);
			for (Result result : Result.values()) {
				results.put(result, new AtomicInteger(0));
			}
			counters.put(objectType, Collections.unmodifiableMap(results));
		}
		COUNTERS = Collections.unmodifiableMap(counters);

		Map<Result, AtomicInteger> cummulative = new EnumMap<Result, AtomicInteger>(Result.class);
		for (Result result : Result.values()) {
			cummulative.put(result, new AtomicInteger(0));
		}
		CUMMULATIVE = Collections.unmodifiableMap(cummulative);
	}

	private CmsCounter() {
	}

	public static int incrementCounter(CmsObject<?> object, Result result) {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to count for"); }
		return CmsCounter.incrementCounter(object.getType(), result);
	}

	public static int incrementCounter(CmsObjectType objectType, Result result) {
		if (objectType == null) { throw new IllegalArgumentException("Unsupported null object type"); }
		if (result == null) { throw new IllegalArgumentException("Must provide a valid result to count for"); }
		CmsCounter.lock.readLock().lock();
		try {
			AtomicInteger counter = CmsCounter.COUNTERS.get(objectType).get(result);
			final int ret = counter.incrementAndGet();
			CmsCounter.CUMMULATIVE.get(result).incrementAndGet();
			return ret;
		} finally {
			CmsCounter.lock.readLock().unlock();
		}
	}

	public static Map<Result, Integer> getCummulativeCounters() {
		return CmsCounter.getCounters((CmsObjectType) null);
	}

	public static Map<Result, Integer> getCounters(CmsObject<?> object) {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to return the counters for"); }
		return CmsCounter.getCounters(object.getType());
	}

	public static Map<Result, Integer> getCounters(CmsObjectType type) {
		Map<Result, Integer> ret = new EnumMap<Result, Integer>(Result.class);
		Map<Result, AtomicInteger> m = (type != null ? CmsCounter.COUNTERS.get(ret) : CmsCounter.CUMMULATIVE);
		CmsCounter.lock.writeLock().lock();
		try {
			for (Map.Entry<Result, AtomicInteger> e : m.entrySet()) {
				ret.put(e.getKey(), e.getValue().get());
			}
			return Collections.unmodifiableMap(ret);
		} finally {
			CmsCounter.lock.writeLock().unlock();
		}
	}

	public static void resetAllCounters() {
		CmsCounter.lock.writeLock().lock();
		try {
			for (CmsObjectType t : CmsObjectType.values()) {
				Map<Result, AtomicInteger> m = CmsCounter.COUNTERS.get(t);
				for (Result r : Result.values()) {
					m.get(r).set(0);
				}
			}
			for (Result r : Result.values()) {
				CmsCounter.CUMMULATIVE.get(r).set(0);
			}
		} finally {
			CmsCounter.lock.writeLock().unlock();
		}
	}

	public static Map<Result, Integer> resetCounters(CmsObject<?> object) {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to return the counters for"); }
		return CmsCounter.getCounters(object.getType());
	}

	public static Map<Result, Integer> resetCounters(CmsObjectType type) {
		Map<Result, Integer> ret = new EnumMap<Result, Integer>(Result.class);
		Map<Result, AtomicInteger> m = (type != null ? CmsCounter.COUNTERS.get(ret) : CmsCounter.CUMMULATIVE);
		CmsCounter.lock.writeLock().lock();
		try {
			for (Map.Entry<Result, AtomicInteger> e : m.entrySet()) {
				final Result r = e.getKey();
				final int val = e.getValue().getAndSet(0);
				ret.put(r, val);
				CUMMULATIVE.get(r).addAndGet(-val);
			}
			return Collections.unmodifiableMap(ret);
		} finally {
			CmsCounter.lock.writeLock().unlock();
		}
	}

	public static String generateCummulativeReport() {
		return CmsCounter.generateCummulativeReport(0);
	}

	public static String generateCummulativeReport(int indentLevel) {
		return CmsCounter.generateReport(CmsCounter.CUMMULATIVE, indentLevel, "Total");
	}

	public static String generateReport(CmsObject<?> object) {
		return CmsCounter.generateReport(object, 0);
	}

	public static String generateReport(CmsObject<?> object, int indentLevel) {
		if (object == null) { throw new IllegalArgumentException(
			"Must provide an object to determine which report to render"); }
		return CmsCounter.generateReport(object.getType(), indentLevel);
	}

	public static String generateReport(CmsObjectType objectType) {
		return CmsCounter.generateReport(objectType, 0);
	}

	public static String generateReport(CmsObjectType objectType, int indentLevel) {
		if (objectType == null) { throw new IllegalArgumentException("Unsupported null object type"); }
		Map<Result, AtomicInteger> results = CmsCounter.COUNTERS.get(objectType);
		return CmsCounter.generateReport(results, indentLevel, String.format("Number of %s", objectType));
	}

	private static String generateReport(Map<Result, AtomicInteger> results, int indentLevel, String entryLabel) {
		CmsCounter.lock.writeLock().lock();
		try {
			StringBuilder s = new StringBuilder();
			if (indentLevel < 0) {
				indentLevel = 0;
			}
			for (int i = 0; i < indentLevel; i++) {
				s.append('\t');
			}
			final String indent = s.toString();
			s.setLength(0);
			int total = 0;
			for (Map.Entry<Result, AtomicInteger> e : results.entrySet()) {
				final Result r = e.getKey();
				final AtomicInteger i = e.getValue();
				total += i.get();
				s.append(indent).append(String.format("%s objects %s: %d%n", entryLabel, r, i.get()));
			}
			s.append(indent).append("========================================").append(CmsCounter.NEW_LINE);
			s.append(indent).append(String.format("%s objects processed: %d%n", entryLabel, total));
			return s.toString();
		} finally {
			CmsCounter.lock.writeLock().unlock();
		}
	}
}