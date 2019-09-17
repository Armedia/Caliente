/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.engine.dynamic.xml;

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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.engine.dynamic.xml.Comparison;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

public class ComparisonTest {

	@Test
	public void testEQ() throws Exception {
		final Comparison comp = Comparison.EQ;
		final Comparison ncomp = Comparison.NEQ;
		final Comparison compi = Comparison.EQI;
		final Comparison ncompi = Comparison.NEQI;
		// Test two known-equal values for equality ... on each data type
		Map<CmfValue.Type, Collection<Pair<?, ?>>> data = new EnumMap<>(CmfValue.Type.class);
		Collection<Pair<?, ?>> pairs = null;

		pairs = new ArrayList<>();
		data.put(CmfValue.Type.INTEGER, pairs);
		pairs.add(Pair.of(Integer.valueOf(1), Integer.valueOf(1)));
		pairs.add(Pair.of(Integer.valueOf(42423), Integer.valueOf(42423)));

		pairs = new ArrayList<>();
		data.put(CmfValue.Type.BOOLEAN, pairs);
		pairs.add(Pair.of(Boolean.valueOf(true), Boolean.valueOf(true)));
		pairs.add(Pair.of(Boolean.valueOf(false), Boolean.valueOf(false)));

		pairs = new ArrayList<>();
		data.put(CmfValue.Type.DOUBLE, pairs);
		pairs.add(Pair.of(Double.valueOf(1.00001), Double.valueOf(1.00001)));
		pairs.add(Pair.of(Float.valueOf(1.00001f), Float.valueOf(1.00001f)));
		pairs.add(Pair.of(Double.valueOf(Double.NaN), Double.valueOf(Double.NaN)));
		pairs.add(Pair.of(Double.valueOf(Double.POSITIVE_INFINITY), Double.valueOf(Double.POSITIVE_INFINITY)));
		pairs.add(Pair.of(Double.valueOf(Double.NEGATIVE_INFINITY), Double.valueOf(Double.NEGATIVE_INFINITY)));

		Calendar c = Calendar.getInstance();
		pairs = new ArrayList<>();
		data.put(CmfValue.Type.DATETIME, pairs);
		pairs.add(Pair.of(new Date(c.getTimeInMillis()), new Date(c.getTimeInMillis())));
		pairs.add(Pair.of(c, c.clone()));
		pairs.add(Pair.of(c, new Date(c.getTimeInMillis())));
		// Remove the milliseconds or this won't work...
		Calendar c2 = Calendar.getInstance();
		c2.setTimeInMillis(c.getTimeInMillis());
		int millis = c2.get(Calendar.MILLISECOND);
		c2.add(Calendar.MILLISECOND, -millis);
		pairs.add(Pair.of(c2, DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.format(c2.getTime())));

		pairs = new ArrayList<>();
		data.put(CmfValue.Type.URI, pairs);
		pairs.add(Pair.of(new URI("http://localhost:80/somepath"), new URI("http://localhost:80/somepath")));

		String uuid = UUID.randomUUID().toString();
		pairs = new ArrayList<>();
		data.put(CmfValue.Type.STRING, pairs);
		pairs.add(Pair.of(new String(uuid), new String(uuid)));

		for (CmfValue.Type t : data.keySet()) {
			for (Pair<?, ?> p : data.get(t)) {
				Assertions.assertTrue(comp.check(t, p.getLeft(), p.getRight()),
					String.format("Equality test failed between [%s] and [%s]", p.getLeft(), p.getRight()));
				Assertions.assertFalse(ncomp.check(t, p.getLeft(), p.getRight()),
					String.format("Inequality test failed between [%s] and [%s]", p.getLeft(), p.getRight()));

				Assertions.assertFalse(comp.check(t, null, p.getRight()),
					String.format("Equality test failed between [%s] and [%s]", null, p.getRight()));
				Assertions.assertTrue(ncomp.check(t, null, p.getRight()),
					String.format("Inequality test failed between [%s] and [%s]", null, p.getRight()));

				if (t == CmfValue.Type.STRING) {
					// Also try the case-insensitive variants
					String left = Tools.toString(p.getLeft()).toLowerCase();
					String right = Tools.toString(p.getRight()).toUpperCase();

					Assertions.assertTrue(compi.check(t, p.getLeft(), p.getRight()),
						String.format("Equality (CI) test failed between [%s] and [%s]", left, right));
					Assertions.assertFalse(ncompi.check(t, p.getLeft(), p.getRight()),
						String.format("Inequality (CI) test failed between [%s] and [%s]", left, right));
				}
			}
		}

		data.clear();

		// Test two known-different values for inequality
		pairs = new ArrayList<>();
		data.put(CmfValue.Type.INTEGER, pairs);
		pairs.add(Pair.of(Integer.valueOf(1), Integer.valueOf(2)));
		pairs.add(Pair.of(Integer.valueOf(42423), Integer.valueOf(42424)));

		pairs = new ArrayList<>();
		data.put(CmfValue.Type.BOOLEAN, pairs);
		pairs.add(Pair.of(Boolean.valueOf(true), Boolean.valueOf(false)));
		pairs.add(Pair.of(Boolean.valueOf(false), Boolean.valueOf(true)));

		pairs = new ArrayList<>();
		data.put(CmfValue.Type.DOUBLE, pairs);
		pairs.add(Pair.of(Double.valueOf(1.00001), Double.valueOf(1.00002)));
		pairs.add(Pair.of(Float.valueOf(1.00002f), Float.valueOf(1.00001f)));
		pairs.add(Pair.of(Double.valueOf(Double.NaN), Double.valueOf(0.0)));
		pairs.add(Pair.of(Double.valueOf(Double.POSITIVE_INFINITY), Double.valueOf(Double.NEGATIVE_INFINITY)));
		pairs.add(Pair.of(Double.valueOf(Double.NEGATIVE_INFINITY), Double.valueOf(Double.POSITIVE_INFINITY)));

		pairs = new ArrayList<>();
		data.put(CmfValue.Type.DATETIME, pairs);
		pairs.add(Pair.of(new Date(System.currentTimeMillis()), new Date(c.getTimeInMillis())));
		pairs.add(Pair.of(c, Calendar.getInstance()));
		pairs.add(Pair.of(c, new Date(System.currentTimeMillis())));
		pairs.add(Pair.of(new Date(System.currentTimeMillis()),
			DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.format(c.getTime())));

		pairs = new ArrayList<>();
		data.put(CmfValue.Type.URI, pairs);
		pairs.add(Pair.of(new URI("http://localhost:80/somepath"), new URI("https://www.google.com:443")));

		pairs = new ArrayList<>();
		data.put(CmfValue.Type.STRING, pairs);
		pairs.add(Pair.of(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
		// Make sure it's case-sensitive

		for (CmfValue.Type t : data.keySet()) {
			for (Pair<?, ?> p : data.get(t)) {
				Assertions.assertFalse(comp.check(t, p.getLeft(), p.getRight()),
					String.format("Inequality test failed between [%s] and [%s]", p.getLeft(), p.getRight()));
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
