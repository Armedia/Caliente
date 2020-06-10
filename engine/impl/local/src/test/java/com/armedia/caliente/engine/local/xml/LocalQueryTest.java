package com.armedia.caliente.engine.local.xml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.lang3.tuple.Triple;
import org.easymock.EasyMock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.commons.utilities.function.CheckedConsumer;

public class LocalQueryTest {

	private static final Object[][] NO_PARAMS = {};
	private static final ResultSetHandler<Object> NO_HANDLER = (rs) -> null;

	@SafeVarargs
	static final <G, S> void testGetter(Consumer<S> setter, Supplier<G> getter,
		Triple<S, Class<? extends Throwable>, Object>... results) {
		LocalQueryTest.testGetter(setter, getter, Arrays.asList(results));
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
		final LocalQuery lq = new LocalQuery();
		List<Triple<String, Class<? extends Throwable>, Object>> data = new LinkedList<>();

		data.add(Triple.of(null, null, null));
		for (int i = 0; i < 10; i++) {
			String value = String.format("sql-%02d", i);
			data.add(Triple.of(value, null, value));
		}

		LocalQueryTest.testGetter(lq::setSql, lq::getSql, data);
	}

	@Test
	public void testSkip() {
		final LocalQuery lq = new LocalQuery();
		List<Triple<Integer, Class<? extends Throwable>, Object>> data = new LinkedList<>();

		data.add(Triple.of(null, null, null));
		for (int i = 0; i < 10; i++) {
			data.add(Triple.of(i, null, i));
		}

		LocalQueryTest.testGetter(lq::setSkip, lq::getSkip, data);
	}

	@Test
	public void testCount() {
		final LocalQuery lq = new LocalQuery();
		List<Triple<Integer, Class<? extends Throwable>, Object>> data = new LinkedList<>();

		data.add(Triple.of(null, null, null));
		for (int i = 0; i < 10; i++) {
			data.add(Triple.of(i, null, i));
		}

		LocalQueryTest.testGetter(lq::setCount, lq::getCount, data);
	}

	@Test
	public void testRelativeTo() {
		final LocalQuery lq = new LocalQuery();
		List<Triple<String, Class<? extends Throwable>, Object>> data = new LinkedList<>();

		data.add(Triple.of(null, null, null));
		for (int i = 0; i < 10; i++) {
			String value = String.format("relative-to-%02d", i);
			data.add(Triple.of(value, null, value));
		}

		LocalQueryTest.testGetter(lq::setRelativeTo, lq::getRelativeTo, data);
	}

	@Test
	public void testId() {
		final LocalQuery lq = new LocalQuery();
		List<Triple<String, Class<? extends Throwable>, Object>> data = new LinkedList<>();

		data.add(Triple.of(null, null, null));
		for (int i = 0; i < 10; i++) {
			String value = String.format("id-%02d", i);
			data.add(Triple.of(value, null, value));
		}

		LocalQueryTest.testGetter(lq::setId, lq::getId, data);
	}

	@Test
	public void testDataSource() {
		final LocalQuery lq = new LocalQuery();
		List<Triple<String, Class<? extends Throwable>, Object>> data = new LinkedList<>();

		data.add(Triple.of(null, null, null));
		for (int i = 0; i < 10; i++) {
			String value = String.format("data-source-%02d", i);
			data.add(Triple.of(value, null, value));
		}

		LocalQueryTest.testGetter(lq::setDataSource, lq::getDataSource, data);
	}

	@Test
	public void testPathColumns() {
		final LocalQuery lq = new LocalQuery();
		List<String> pc = lq.getPathColumns();
		for (int i = 0; i < 100; i++) {
			Assertions.assertSame(pc, lq.getPathColumns());
		}
	}

	@Test
	public void testPostProcessors() {
		final LocalQuery lq = new LocalQuery();
		List<LocalQueryPostProcessor> pp = lq.getPostProcessors();
		for (int i = 0; i < 100; i++) {
			Assertions.assertSame(pp, lq.getPostProcessors());
		}
	}

	@SafeVarargs
	private final BasicDataSource buildDataSource(CheckedConsumer<QueryRunner, ? extends Exception>... initializers)
		throws Exception {
		BasicDataSource bds = new BasicDataSource();
		bds.setUrl("jdbc:h2:mem:testDataSource-" + UUID.randomUUID().toString());
		bds.setDriverClassName("org.h2.Driver");

		if (initializers.length > 0) {
			final QueryRunner qr = new QueryRunner(bds);
			try (Connection c = bds.getConnection()) {
				c.setAutoCommit(false);
				for (CheckedConsumer<QueryRunner, ? extends Exception> i : initializers) {
					i.accept(qr);
					c.commit();
				}
				c.setAutoCommit(true);
			}
		}
		return bds;
	}

	private Stream<Path> loadPaths(String resource) throws IOException {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		InputStream in = cl.getResourceAsStream(resource);
		return new LineNumberReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines().map(Paths::get);
	}

	// paths-1 is the happy path - one column, bunch of rows, no nulls, no nothing
	private void renderFirstPaths(QueryRunner qr) throws Exception {
		qr.update("create table paths_one ( path varchar(2048) not null, primary key (path) )");
		List<Object[]> params = new ArrayList<>();
		loadPaths("paths-1.txt").forEach((p) -> {
			params.add(new String[] {
				p.toString()
			});
		});
		Object[][] paramsArray = params.toArray(LocalQueryTest.NO_PARAMS);
		qr.insertBatch("insert into paths_one (path) values (?)", LocalQueryTest.NO_HANDLER, paramsArray);
	}

	// paths-2 requires relativization
	private void renderSecondPaths(QueryRunner qr) throws Exception {
		qr.update("create table paths_two ( one varchar(2048), two varchar(2048), three varchar(2048) )");
		Path current = Paths.get(".").toRealPath();
		List<Object[]> params = new ArrayList<>();
		final AtomicInteger counter = new AtomicInteger(0);
		loadPaths("paths-2.txt").forEach((p) -> {
			String[] data = new String[3];
			data[counter.getAndIncrement() % 3] = current.resolve(p).toString();
			params.add(data);
		});
		Object[][] paramsArray = params.toArray(LocalQueryTest.NO_PARAMS);
		qr.insertBatch("insert into paths_two (one, two, three) values (?, ?, ?)", LocalQueryTest.NO_HANDLER,
			paramsArray);
	}

	// paths-3 requires relativization
	private void renderThirdPaths(QueryRunner qr) throws Exception {
		qr.update("create table paths_three ( path varchar(2048) )");
		Path current = Paths.get(".").toRealPath();
		Path alternate = Paths.get(".").toRealPath().getParent();
		List<Object[]> params = new ArrayList<>();
		final AtomicInteger counter = new AtomicInteger(0);
		loadPaths("paths-3.txt").forEach((p) -> {
			final int c = counter.getAndIncrement();
			Path parent = ((c % 2) == 0 ? current : alternate);
			params.add(new String[] {
				parent.resolve(p).toString()
			});
		});
		Object[][] paramsArray = params.toArray(LocalQueryTest.NO_PARAMS);
		qr.insertBatch("insert into paths_three (path) values (?)", LocalQueryTest.NO_HANDLER, paramsArray);
	}

	@Test
	public void testStream() throws Exception {
		final LocalQuery lq = new LocalQuery();

		final DataSource mockDS = EasyMock.createStrictMock(DataSource.class);

		// First things first: what happens with null arguments?
		Assertions.assertThrows(NullPointerException.class, () -> lq.getStream(null));

		@SuppressWarnings("resource")
		Stream<Path> s = null;

		Assertions.assertThrows(SQLException.class, () -> lq.getStream(mockDS));

		lq.getPathColumns().add("1");

		Assertions.assertNotNull(s = lq.getStream(mockDS));
		s.close();

		lq.setSkip(null);
		Assertions.assertNotNull(s = lq.getStream(mockDS));
		s.close();
		lq.setSkip(-1);
		Assertions.assertNotNull(s = lq.getStream(mockDS));
		s.close();
		lq.setSkip(0);
		Assertions.assertNotNull(s = lq.getStream(mockDS));
		s.close();
		lq.setSkip(1);
		Assertions.assertNotNull(s = lq.getStream(mockDS));
		s.close();

		lq.setCount(null);
		Assertions.assertNotNull(s = lq.getStream(mockDS));
		s.close();
		lq.setCount(-1);
		Assertions.assertNotNull(s = lq.getStream(mockDS));
		s.close();
		lq.setCount(0);
		Assertions.assertNotNull(s = lq.getStream(mockDS));
		s.close();
		lq.setCount(1);
		Assertions.assertNotNull(s = lq.getStream(mockDS));
		s.close();

		final Path current = Paths.get(".").toRealPath();
		lq.setRelativeTo(null);
		Assertions.assertNotNull(s = lq.getStream(mockDS));
		s.close();
		lq.setRelativeTo(current.toString());
		Assertions.assertNotNull(s = lq.getStream(mockDS));
		s.close();

		final Set<Path> baseLinesOne = loadPaths("paths-1.txt").collect(Collectors.toCollection(LinkedHashSet::new));
		final Set<Path> baseLinesTwo = loadPaths("paths-2.txt").collect(Collectors.toCollection(LinkedHashSet::new));
		final Set<Path> baseLinesThree = loadPaths("paths-3.txt").collect(Collectors.toCollection(LinkedHashSet::new));

		final Set<Path> lines = new HashSet<>();
		final Set<Path> found = new LinkedHashSet<>();

		try (BasicDataSource dataSource = buildDataSource(this::renderFirstPaths)) {
			lq.setSkip(0);
			lq.setCount(-1);

			lq.setId("someId");
			lq.setDataSource("someDataSource");
			lq.setRelativeTo(null);

			lq.getPathColumns().clear();
			lq.getPathColumns().add("1");
			lq.getPathColumns().add("path");
			lq.getPathColumns().add("-13");
			lq.getPathColumns().add("13");
			lq.getPathColumns().add("garbage");

			lq.setSql("select path from paths_one");

			lines.clear();
			lines.addAll(baseLinesOne);
			found.clear();

			try (Stream<Path> targets = lq.getStream(dataSource)) {
				targets.forEach((str) -> {
					Assertions.assertTrue(lines.remove(str));
					found.add(str);
				});
				Assertions.assertTrue(lines.isEmpty());
				Assertions.assertEquals(baseLinesOne.size(), found.size());
			}
		}

		try (BasicDataSource dataSource = buildDataSource(this::renderSecondPaths)) {
			lq.setSkip(0);
			lq.setCount(-1);

			lq.setId("someId");
			lq.setDataSource("someDataSource");
			lq.setRelativeTo(current.toString());

			lq.getPathColumns().clear();
			lq.getPathColumns().add("one");
			lq.getPathColumns().add("2");
			lq.getPathColumns().add("three");

			lq.setSql("select * from paths_two");

			lines.clear();
			lines.addAll(baseLinesTwo);
			found.clear();

			try (Stream<Path> targets = lq.getStream(dataSource)) {
				targets.forEach((str) -> {
					Assertions.assertTrue(lines.remove(str));
					found.add(str);
				});
				Assertions.assertTrue(lines.isEmpty());
				Assertions.assertEquals(baseLinesTwo.size(), found.size());
			}
		}

		try (BasicDataSource dataSource = buildDataSource(this::renderThirdPaths)) {
			lq.setSkip(0);
			lq.setCount(-1);

			lq.setId("someId");
			lq.setDataSource("someDataSource");
			lq.setRelativeTo(current.toString());

			lq.getPathColumns().clear();
			lq.getPathColumns().add("path");

			lq.setSql("select * from paths_three");

			lines.clear();
			lines.addAll(baseLinesThree);
			found.clear();

			try (Stream<Path> targets = lq.getStream(dataSource)) {
				targets.forEach((str) -> {
					Assertions.assertTrue(lines.remove(str));
					found.add(str);
				});
				Assertions.assertFalse(lines.isEmpty());
				Assertions.assertFalse(found.isEmpty());
				Assertions.assertEquals(baseLinesThree.size(), lines.size() + found.size());
			}
		}

		try (BasicDataSource dataSource = buildDataSource(this::renderFirstPaths)) {

			LocalQueryPostProcessor lqpp = new LocalQueryPostProcessor();
			lqpp.setType("jexl3");
			lqpp.setValue("return '" + current.toString() + File.separatorChar + "' + path");

			lq.setSkip(0);
			lq.setCount(-1);

			lq.setId("someId");
			lq.setDataSource("someDataSource");
			lq.setRelativeTo(current.toString());

			lq.getPathColumns().clear();
			lq.getPathColumns().add("path");

			lq.getPostProcessors().clear();
			lq.getPostProcessors().add(lqpp);

			lq.setSql("select * from paths_one");

			lines.clear();
			lines.addAll(baseLinesOne);
			found.clear();

			try (Stream<Path> targets = lq.getStream(dataSource)) {
				targets.forEach((str) -> {
					Assertions.assertTrue(lines.remove(str));
					found.add(str);
				});
				Assertions.assertTrue(lines.isEmpty());
				Assertions.assertEquals(baseLinesOne.size(), found.size());
			}
		}
	}
}