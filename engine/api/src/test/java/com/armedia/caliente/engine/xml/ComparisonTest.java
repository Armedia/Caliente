package com.armedia.caliente.engine.xml;

import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;

import com.armedia.caliente.engine.dynamic.xml.Comparison;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.commons.utilities.Tools;

public class ComparisonTest {

	@Test
	public void testEQ() throws Exception {
		final Comparison comp = Comparison.EQ;
		final Comparison ncomp = Comparison.NEQ;
		final Comparison compi = Comparison.EQI;
		final Comparison ncompi = Comparison.NEQI;
		// Test two known-equal values for equality ... on each data type
		Map<CmfDataType, Collection<Pair<?, ?>>> data = new EnumMap<>(CmfDataType.class);
		Collection<Pair<?, ?>> pairs = null;

		pairs = new ArrayList<>();
		data.put(CmfDataType.INTEGER, pairs);
		pairs.add(Pair.of(new Integer(1), new Integer(1)));
		pairs.add(Pair.of(new Integer(42423), new Integer(42423)));

		pairs = new ArrayList<>();
		data.put(CmfDataType.BOOLEAN, pairs);
		pairs.add(Pair.of(new Boolean(true), new Boolean(true)));
		pairs.add(Pair.of(new Boolean(false), new Boolean(false)));

		pairs = new ArrayList<>();
		data.put(CmfDataType.DOUBLE, pairs);
		pairs.add(Pair.of(new Double(1.00001), new Double(1.00001)));
		pairs.add(Pair.of(new Float(1.00001), new Float(1.00001)));
		pairs.add(Pair.of(new Double(Double.NaN), new Double(Double.NaN)));
		pairs.add(Pair.of(new Double(Double.POSITIVE_INFINITY), new Double(Double.POSITIVE_INFINITY)));
		pairs.add(Pair.of(new Double(Double.NEGATIVE_INFINITY), new Double(Double.NEGATIVE_INFINITY)));

		Calendar c = Calendar.getInstance();
		pairs = new ArrayList<>();
		data.put(CmfDataType.DATETIME, pairs);
		pairs.add(Pair.of(new Date(c.getTimeInMillis()), new Date(c.getTimeInMillis())));
		pairs.add(Pair.of(c, c.clone()));
		pairs.add(Pair.of(c, new Date(c.getTimeInMillis())));
		// Remove the milliseconds or this won't work...
		Calendar c2 = Calendar.getInstance();
		c2.setTimeInMillis(c.getTimeInMillis());
		int millis = c2.get(Calendar.MILLISECOND);
		c2.add(Calendar.MILLISECOND, -millis);
		pairs.add(Pair.of(c2, DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(c2.getTime())));

		pairs = new ArrayList<>();
		data.put(CmfDataType.URI, pairs);
		pairs.add(Pair.of(new URI("http://localhost:80/somepath"), new URI("http://localhost:80/somepath")));

		String uuid = UUID.randomUUID().toString();
		pairs = new ArrayList<>();
		data.put(CmfDataType.STRING, pairs);
		pairs.add(Pair.of(new String(uuid), new String(uuid)));

		for (CmfDataType t : data.keySet()) {
			for (Pair<?, ?> p : data.get(t)) {
				Assert.assertTrue(
					String.format("Equality test failed between [%s] and [%s]", p.getLeft(), p.getRight()),
					comp.check(t, p.getLeft(), p.getRight()));
				Assert.assertFalse(
					String.format("Inequality test failed between [%s] and [%s]", p.getLeft(), p.getRight()),
					ncomp.check(t, p.getLeft(), p.getRight()));

				Assert.assertFalse(String.format("Equality test failed between [%s] and [%s]", null, p.getRight()),
					comp.check(t, null, p.getRight()));
				Assert.assertTrue(String.format("Inequality test failed between [%s] and [%s]", null, p.getRight()),
					ncomp.check(t, null, p.getRight()));

				if (t == CmfDataType.STRING) {
					// Also try the case-insensitive variants
					String left = Tools.toString(p.getLeft()).toLowerCase();
					String right = Tools.toString(p.getRight()).toUpperCase();

					Assert.assertTrue(String.format("Equality (CI) test failed between [%s] and [%s]", left, right),
						compi.check(t, p.getLeft(), p.getRight()));
					Assert.assertFalse(String.format("Inequality (CI) test failed between [%s] and [%s]", left, right),
						ncompi.check(t, p.getLeft(), p.getRight()));
				}
			}
		}

		data.clear();

		// Test two known-different values for inequality
		pairs = new ArrayList<>();
		data.put(CmfDataType.INTEGER, pairs);
		pairs.add(Pair.of(new Integer(1), new Integer(2)));
		pairs.add(Pair.of(new Integer(42423), new Integer(42424)));

		pairs = new ArrayList<>();
		data.put(CmfDataType.BOOLEAN, pairs);
		pairs.add(Pair.of(new Boolean(true), new Boolean(false)));
		pairs.add(Pair.of(new Boolean(false), new Boolean(true)));

		pairs = new ArrayList<>();
		data.put(CmfDataType.DOUBLE, pairs);
		pairs.add(Pair.of(new Double(1.00001), new Double(1.00002)));
		pairs.add(Pair.of(new Float(1.00002), new Float(1.00001)));
		pairs.add(Pair.of(new Double(Double.NaN), new Double(0.0)));
		pairs.add(Pair.of(new Double(Double.POSITIVE_INFINITY), new Double(Double.NEGATIVE_INFINITY)));
		pairs.add(Pair.of(new Double(Double.NEGATIVE_INFINITY), new Double(Double.POSITIVE_INFINITY)));

		pairs = new ArrayList<>();
		data.put(CmfDataType.DATETIME, pairs);
		pairs.add(Pair.of(new Date(System.currentTimeMillis()), new Date(c.getTimeInMillis())));
		pairs.add(Pair.of(c, Calendar.getInstance()));
		pairs.add(Pair.of(c, new Date(System.currentTimeMillis())));
		pairs.add(Pair.of(new Date(System.currentTimeMillis()),
			DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(c.getTime())));

		pairs = new ArrayList<>();
		data.put(CmfDataType.URI, pairs);
		pairs.add(Pair.of(new URI("http://localhost:80/somepath"), new URI("https://www.google.com:443")));

		pairs = new ArrayList<>();
		data.put(CmfDataType.STRING, pairs);
		pairs.add(Pair.of(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
		// Make sure it's case-sensitive

		for (CmfDataType t : data.keySet()) {
			for (Pair<?, ?> p : data.get(t)) {
				Assert.assertFalse(
					String.format("Inequality test failed between [%s] and [%s]", p.getLeft(), p.getRight()),
					comp.check(t, p.getLeft(), p.getRight()));
			}
		}
	}

	@Test
	public void testGT() {
	}

	@Test
	public void testGE() {
	}

	@Test
	public void testLT() {
	}

	@Test
	public void testLE() {
	}

	@Test
	public void testSW() {
	}

	@Test
	public void testEW() {
	}

	@Test
	public void testCN() {
	}

	@Test
	public void testRE() {
	}

	@Test
	public void testGLOB() {
	}
}
