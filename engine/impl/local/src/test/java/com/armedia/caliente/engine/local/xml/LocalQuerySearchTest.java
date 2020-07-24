package com.armedia.caliente.engine.local.xml;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LocalQuerySearchTest {

	@SafeVarargs
	static final <G, S> void testGetter(Consumer<S> setter, Supplier<G> getter,
		Triple<S, Class<? extends Throwable>, Object>... results) {
		LocalQuerySearchTest.testGetter(setter, getter, Arrays.asList(results));
	}

	static final <G, S> void testGetter(Consumer<S> setter, Supplier<G> getter,
		Collection<Triple<S, Class<? extends Throwable>, Object>> results) {
		Assertions.assertNotNull(results);
		for (Triple<S, Class<? extends Throwable>, Object> t : results) {
			final S s = t.getLeft();
			final Class<? extends Throwable> e = t.getMiddle();
			final Object g = t.getRight();

			if (setter != null) {
				// First, test the setter... the exception is
				// what the setter is expected to throw
				if (e != null) {
					// Expect an exception
					Assertions.assertThrows(e, () -> setter.accept(s));
					continue;
				}

				// No exception in the setter, so we set the value and check to see
				// what the getter is expected to do
				setter.accept(s);
			}

			if (getter != null) {

				// Simple path: null result
				if (g == null) {
					Assertions.assertNull(getter.get());
					continue;
				}

				// Is the result an expected exception class?
				if (Class.class.isInstance(g)) {
					Class<?> c = Class.class.cast(g);
					if (!Throwable.class.isAssignableFrom(c)) {
						Assertions.fail(
							String.format("A non-exception %s was submitted as part of a result group: %s", c, t));
					}
					@SuppressWarnings("unchecked")
					Class<? extends Throwable> eg = (Class<? extends Throwable>) c;
					Assertions.assertThrows(eg, getter::get);
					continue;
				}

				// Not an exception, not null, do an "equals" comparison
				Assertions.assertEquals(g, getter.get());
			}
		}
	}

	@Test
	public void testSql() {
		final LocalQuerySearch lq = new LocalQuerySearch();
		List<Triple<String, Class<? extends Throwable>, Object>> data = new LinkedList<>();

		data.add(Triple.of(null, null, null));
		for (int i = 0; i < 10; i++) {
			String value = String.format("sql-%02d", i);
			data.add(Triple.of(value, null, value));
		}

		LocalQuerySearchTest.testGetter(lq::setSql, lq::getSql, data);
	}

	@Test
	public void testSkip() {
		final LocalQuerySearch lq = new LocalQuerySearch();
		List<Triple<Integer, Class<? extends Throwable>, Object>> data = new LinkedList<>();

		data.add(Triple.of(null, null, null));
		for (int i = 0; i < 10; i++) {
			data.add(Triple.of(i, null, i));
		}

		LocalQuerySearchTest.testGetter(lq::setSkip, lq::getSkip, data);
	}

	@Test
	public void testCount() {
		final LocalQuerySearch lq = new LocalQuerySearch();
		List<Triple<Integer, Class<? extends Throwable>, Object>> data = new LinkedList<>();

		data.add(Triple.of(null, null, null));
		for (int i = 0; i < 10; i++) {
			data.add(Triple.of(i, null, i));
		}

		LocalQuerySearchTest.testGetter(lq::setCount, lq::getCount, data);
	}

	@Test
	public void testId() {
		final LocalQuerySearch lq = new LocalQuerySearch();
		List<Triple<String, Class<? extends Throwable>, Object>> data = new LinkedList<>();

		data.add(Triple.of(null, null, null));
		for (int i = 0; i < 10; i++) {
			String value = String.format("id-%02d", i);
			data.add(Triple.of(value, null, value));
		}

		LocalQuerySearchTest.testGetter(lq::setId, lq::getId, data);
	}

	@Test
	public void testDataSource() {
		final LocalQuerySearch lq = new LocalQuerySearch();
		List<Triple<String, Class<? extends Throwable>, Object>> data = new LinkedList<>();

		data.add(Triple.of(null, null, null));
		for (int i = 0; i < 10; i++) {
			String value = String.format("data-source-%02d", i);
			data.add(Triple.of(value, null, value));
		}

		LocalQuerySearchTest.testGetter(lq::setDataSource, lq::getDataSource, data);
	}

	@Test
	public void testPathColumns() {
		final LocalQuerySearch lq = new LocalQuerySearch();
		List<String> pc = lq.getPathColumns();
		for (int i = 0; i < 100; i++) {
			Assertions.assertSame(pc, lq.getPathColumns());
		}
	}

	@Test
	public void testPostProcessors() {
		final LocalQuerySearch lq = new LocalQuerySearch();
		List<LocalQueryPostProcessor> pp = lq.getPostProcessors();
		for (int i = 0; i < 100; i++) {
			Assertions.assertSame(pp, lq.getPostProcessors());
		}
	}
}