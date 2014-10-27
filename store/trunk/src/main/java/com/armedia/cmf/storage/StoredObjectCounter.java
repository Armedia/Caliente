package com.armedia.cmf.storage;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang3.StringUtils;

public final class StoredObjectCounter<T extends Enum<T>, R extends Enum<R>> {

	private static final String TOTAL_LABEL = "processed".intern();
	private static final String NEW_LINE = String.format("%n");

	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private final Map<T, Map<R, AtomicInteger>> counters;
	private final Map<R, AtomicInteger> cummulative;
	private final Class<T> tClass;
	private final Class<R> rClass;
	private final String formatString;

	public StoredObjectCounter(Class<T> tClass, Class<R> rClass) {
		if (tClass == null) { throw new IllegalArgumentException("Must provide an enum class for the object type"); }
		if (rClass == null) { throw new IllegalArgumentException("Must provide an enum class for the result"); }
		this.rClass = rClass;
		this.tClass = tClass;

		int maxWidth = 0;
		Map<R, AtomicInteger> cummulative = new EnumMap<R, AtomicInteger>(rClass);
		for (R result : this.rClass.getEnumConstants()) {
			cummulative.put(result, new AtomicInteger(0));
			maxWidth = Math.max(maxWidth, result.name().length());
		}
		this.cummulative = Collections.unmodifiableMap(cummulative);

		Map<T, Map<R, AtomicInteger>> counters = new EnumMap<T, Map<R, AtomicInteger>>(tClass);
		for (T type : tClass.getEnumConstants()) {
			Map<R, AtomicInteger> results = new EnumMap<R, AtomicInteger>(rClass);
			for (R result : this.rClass.getEnumConstants()) {
				results.put(result, new AtomicInteger(0));
			}
			counters.put(type, Collections.unmodifiableMap(results));
		}
		this.counters = Collections.unmodifiableMap(counters);

		maxWidth = Math.max(maxWidth, StoredObjectCounter.TOTAL_LABEL.length());
		this.formatString = String.format("%%s objects %%-%ds: %%6d%%n", maxWidth);
	}

	public int increment(T type, R result) {
		if (type == null) { throw new IllegalArgumentException("Unsupported null object type"); }
		if (result == null) { throw new IllegalArgumentException("Must provide a valid result to count for"); }
		this.lock.readLock().lock();
		try {
			AtomicInteger counter = this.counters.get(type).get(result);
			final int ret = counter.incrementAndGet();
			this.cummulative.get(result).incrementAndGet();
			return ret;
		} finally {
			this.lock.readLock().unlock();
		}
	}

	public Map<R, Integer> getCummulative() {
		return getCounters(null);
	}

	public Map<R, Integer> getCounters(T type) {
		Map<R, Integer> ret = new EnumMap<R, Integer>(this.rClass);
		Map<R, AtomicInteger> m = (type != null ? this.counters.get(type) : this.cummulative);
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
			for (T t : this.tClass.getEnumConstants()) {
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

	public Map<R, Integer> reset(T type) {
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

	public String generateReport(T type) {
		return generateReport(type, 0);
	}

	public String generateReport(T type, int indentLevel) {
		if (type == null) { throw new IllegalArgumentException("Unsupported null object type"); }
		Map<R, AtomicInteger> results = this.counters.get(type);
		return generateReport(results, indentLevel, String.format("Number of %s", type));
	}

	private String generateReport(Map<R, AtomicInteger> results, int indentLevel, String entryLabel) {
		this.lock.writeLock().lock();
		try {
			return StoredObjectCounter.generateSummary(this.rClass, results, indentLevel, entryLabel, StoredObjectCounter.TOTAL_LABEL,
				this.formatString);
		} finally {
			this.lock.writeLock().unlock();
		}
	}

	private static <E extends Enum<E>> String calculateFormatString(Class<E> klass, String totalLabel) {
		int maxWidth = 0;
		for (E e : klass.getEnumConstants()) {
			maxWidth = Math.max(maxWidth, e.name().length());
		}
		maxWidth = Math.max(maxWidth, totalLabel.length());
		return String.format("%%s objects %%-%ds: %%6d%%n", maxWidth);
	}

	public static <E extends Enum<E>> String generateSummary(Class<E> klass, Map<E, ? extends Number> results,
		int indentLevel, String entryLabel, String totalLabel) {
		return StoredObjectCounter.generateSummary(klass, results, indentLevel, entryLabel, totalLabel, null);
	}

	public static <E extends Enum<E>> String generateSummary(Class<E> klass, Map<E, ? extends Number> results,
		int indentLevel, String entryLabel, String totalLabel, String formatString) {

		if (formatString == null) {
			formatString = StoredObjectCounter.calculateFormatString(klass, totalLabel);
		}

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
		for (Map.Entry<E, ? extends Number> e : results.entrySet()) {
			final E r = e.getKey();
			final Number i = e.getValue();
			int intValue = i.intValue();
			total += intValue;
			s.append(indent).append(String.format(formatString, entryLabel, r, intValue));
		}
		String totalLine = String.format(formatString, entryLabel, totalLabel, total);
		// PATCH: need to repeat one less than the length of the line, or we'll overflow by 1...
		s.append(indent).append(StringUtils.repeat("=", totalLine.length() - 1)).append(StoredObjectCounter.NEW_LINE);
		s.append(indent).append(totalLine).append(StoredObjectCounter.NEW_LINE);
		return s.toString();
	}
}