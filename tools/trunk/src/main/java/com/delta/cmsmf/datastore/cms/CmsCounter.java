package com.delta.cmsmf.datastore.cms;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class CmsCounter {

	public static enum Result {
		//
		READ,
		SKIPPED,
		CREATED,
		UPDATED,
		FAILED;
	}

	private static final Map<CmsObjectType, Map<Result, AtomicInteger>> COUNTERS;

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
		return CmsCounter.COUNTERS.get(objectType).get(result).incrementAndGet();
	}

	public static String generateCummulativeReport() {
		return CmsCounter.generateCummulativeReport(0);
	}

	public static String generateCummulativeReport(int indentLevel) {
		StringBuilder s = new StringBuilder();
		if (indentLevel < 0) {
			indentLevel = 0;
		}
		for (int i = 0; i < indentLevel; i++) {
			s.append('\t');
		}
		final String indent = s.toString();
		s.setLength(0);
		Map<Result, AtomicInteger> cummulative = new EnumMap<Result, AtomicInteger>(Result.class);
		for (CmsObjectType objectType : CmsObjectType.values()) {
			Map<Result, AtomicInteger> results = CmsCounter.COUNTERS.get(objectType);
			for (Map.Entry<Result, AtomicInteger> e : results.entrySet()) {
				final Result r = e.getKey();
				final AtomicInteger i = e.getValue();
				AtomicInteger c = cummulative.get(r);
				if (c == null) {
					c = new AtomicInteger(0);
					cummulative.put(r, c);
				}
				c.addAndGet(i.get());
			}
		}

		for (Map.Entry<Result, AtomicInteger> e : cummulative.entrySet()) {
			final Result r = e.getKey();
			final AtomicInteger i = e.getValue();
			if (indentLevel > 0) {
				s.append(indent);
			}
			s.append(String.format("Total number of objects %s: %d%n", r, i.get()));
		}
		return s.toString();
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
		StringBuilder s = new StringBuilder();
		if (indentLevel < 0) {
			indentLevel = 0;
		}
		for (int i = 0; i < indentLevel; i++) {
			s.append('\t');
		}
		final String indent = s.toString();
		s.setLength(0);
		for (Map.Entry<Result, AtomicInteger> e : results.entrySet()) {
			final Result r = e.getKey();
			final AtomicInteger i = e.getValue();
			if (indentLevel > 0) {
				s.append(indent);
			}
			s.append(String.format("Number of %s objects %s: %d%n", objectType, r, i.get()));
		}
		return s.toString();
	}
}