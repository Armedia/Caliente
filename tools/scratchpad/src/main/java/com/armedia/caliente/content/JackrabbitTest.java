package com.armedia.caliente.content;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

public class JackrabbitTest implements Callable<Void> {

	@Override
	public Void call() throws Exception {
		Map<Integer, AtomicInteger> m = new LinkedHashMap<>();
		System.out.printf("Generating hashes...%n");
		for (long i = 0; i < 177550000; i++) {
			String s = String.format("%016x", i);
			int hash = s.hashCode();
			AtomicInteger counter = m.get(hash);
			if (counter == null) {
				counter = new AtomicInteger(0);
				m.put(hash, counter);
			}
			counter.incrementAndGet();
			if ((i % 10000) == 0) {
				System.out.printf("\tPos = %d%n", i);
			}
		}

		System.out.printf("Total buckets: %d%n", m.size());

		SummaryStatistics stats = new SummaryStatistics();

		Integer minHash = null;
		int min = -1;
		Integer maxHash = null;
		int max = 0;

		for (Integer i : m.keySet()) {
			int size = m.get(i).get();
			stats.addValue(size);
			if ((minHash == null) || (size < min)) {
				min = size;
				minHash = i;
			}
			if ((maxHash == null) || (size > max)) {
				max = size;
				maxHash = i;
			}
		}
		System.out.printf("Min hash: %08x (%d)%n", minHash.intValue(), min);
		System.out.printf("Max hash: %08x (%d)%n", maxHash.intValue(), max);
		System.out.printf("Average: %.3f (stddev = %.3f)%n", stats.getMean(), stats.getStandardDeviation());

		/*
		Repository repository = JcrUtils.getRepository();
		Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
		try {
		
			Node root = session.getRootNode();
		
			Node base = root.addNode("base");
			Node child = base.addNode("child");
			Node grandchild = child.addNode("grandchild");
			session.save();
		
		} finally {
			session.logout();
		}
		*/
		return null;
	}

}