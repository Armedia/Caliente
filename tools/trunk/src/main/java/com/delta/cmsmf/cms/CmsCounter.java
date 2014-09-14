package com.delta.cmsmf.cms;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class CmsCounter<R extends Enum<R>> {

	private static final String NEW_LINE = String.format("%n");

	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private final Map<CmsObjectType, Map<R, AtomicInteger>> counters;
	private final Map<R, AtomicInteger> cummulative;
	private final Class<R> rClass;

	public CmsCounter(Class<R> rClass) {
		if (rClass == null) { throw new IllegalArgumentException("Must provide an enum class"); }
		this.rClass = rClass;
		Map<CmsObjectType, Map<R, AtomicInteger>> counters = new EnumMap<CmsObjectType, Map<R, AtomicInteger>>(
			CmsObjectType.class);
		for (CmsObjectType objectType : CmsObjectType.values()) {
			Map<R, AtomicInteger> results = new EnumMap<R, AtomicInteger>(rClass);
			for (R result : this.rClass.getEnumConstants()) {
				results.put(result, new AtomicInteger(0));
			}
			counters.put(objectType, Collections.unmodifiableMap(results));
		}
		this.counters = Collections.unmodifiableMap(counters);

		Map<R, AtomicInteger> cummulative = new EnumMap<R, AtomicInteger>(rClass);
		for (R result : this.rClass.getEnumConstants()) {
			cummulative.put(result, new AtomicInteger(0));
		}
		this.cummulative = Collections.unmodifiableMap(cummulative);
	}

	public int increment(CmsObject<?> object, R result) {
		if (object == null) { throw new IllegalArgumentException("Must provide an object whose type to count for"); }
		return increment(object.getType(), result);
	}

	public int increment(CmsObjectType objectType, R result) {
		if (objectType == null) { throw new IllegalArgumentException("Unsupported null object type"); }
		if (result == null) { throw new IllegalArgumentException("Must provide a valid result to count for"); }
		this.lock.readLock().lock();
		try {
			AtomicInteger counter = this.counters.get(objectType).get(result);
			final int ret = counter.incrementAndGet();
			this.cummulative.get(result).incrementAndGet();
			return ret;
		} finally {
			this.lock.readLock().unlock();
		}
	}

	public Map<R, Integer> getCummulative() {
		return getCounters((CmsObjectType) null);
	}

	public Map<R, Integer> getCounters(CmsObject<?> object) {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to return the counters for"); }
		return getCounters(object.getType());
	}

	public Map<R, Integer> getCounters(CmsObjectType type) {
		Map<R, Integer> ret = new EnumMap<R, Integer>(this.rClass);
		Map<R, AtomicInteger> m = (type != null ? this.counters.get(ret) : this.cummulative);
		this.lock.writeLock().lock();
		try {
			for (Map.Entry<R, AtomicInteger> e : m.entrySet()) {
				ret.put(e.getKey(), e.getValue().get());
			}
			return Collections.unmodifiableMap(ret);
		} finally {
			this.lock.writeLock().unlock();
		}
	}

	public void reset() {
		this.lock.writeLock().lock();
		try {
			for (CmsObjectType t : CmsObjectType.values()) {
				Map<R, AtomicInteger> m = this.counters.get(t);
				for (R r : this.rClass.getEnumConstants()) {
					m.get(r).set(0);
				}
			}
			for (R r : this.rClass.getEnumConstants()) {
				this.cummulative.get(r).set(0);
			}
		} finally {
			this.lock.writeLock().unlock();
		}
	}

	public Map<R, Integer> reset(CmsObject<?> object) {
		if (object == null) { throw new IllegalArgumentException(
			"Must provide an object whose type to reset the counters for"); }
		return reset(object.getType());
	}

	public Map<R, Integer> reset(CmsObjectType type) {
		Map<R, Integer> ret = new EnumMap<R, Integer>(this.rClass);
		Map<R, AtomicInteger> m = (type != null ? this.counters.get(ret) : this.cummulative);
		this.lock.writeLock().lock();
		try {
			for (Map.Entry<R, AtomicInteger> e : m.entrySet()) {
				final R r = e.getKey();
				final int val = e.getValue().getAndSet(0);
				ret.put(r, val);
				this.cummulative.get(r).addAndGet(-val);
			}
			return Collections.unmodifiableMap(ret);
		} finally {
			this.lock.writeLock().unlock();
		}
	}

	public String generateCummulativeReport() {
		return generateCummulativeReport(0);
	}

	public String generateCummulativeReport(int indentLevel) {
		return generateReport(this.cummulative, indentLevel, "Total");
	}

	public String generateReport(CmsObject<?> object) {
		return generateReport(object, 0);
	}

	public String generateReport(CmsObject<?> object, int indentLevel) {
		if (object == null) { throw new IllegalArgumentException(
			"Must provide an object to determine which report to render"); }
		return generateReport(object.getType(), indentLevel);
	}

	public String generateReport(CmsObjectType objectType) {
		return generateReport(objectType, 0);
	}

	public String generateReport(CmsObjectType objectType, int indentLevel) {
		if (objectType == null) { throw new IllegalArgumentException("Unsupported null object type"); }
		Map<R, AtomicInteger> results = this.counters.get(objectType);
		return generateReport(results, indentLevel, String.format("Number of %s", objectType));
	}

	private String generateReport(Map<R, AtomicInteger> results, int indentLevel, String entryLabel) {
		this.lock.writeLock().lock();
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
			for (Map.Entry<R, AtomicInteger> e : results.entrySet()) {
				final R r = e.getKey();
				final AtomicInteger i = e.getValue();
				total += i.get();
				s.append(indent).append(String.format("%s objects %s: %d%n", entryLabel, r, i.get()));
			}
			s.append(indent).append("========================================").append(CmsCounter.NEW_LINE);
			s.append(indent).append(String.format("%s objects processed: %d%n", entryLabel, total));
			return s.toString();
		} finally {
			this.lock.writeLock().unlock();
		}
	}
}