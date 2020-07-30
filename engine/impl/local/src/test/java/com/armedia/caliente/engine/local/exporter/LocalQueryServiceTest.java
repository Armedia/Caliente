package com.armedia.caliente.engine.local.exporter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.lang3.tuple.Pair;
import org.easymock.EasyMock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.engine.local.common.LocalCommon;
import com.armedia.caliente.engine.local.exporter.LocalQueryService.Processor;
import com.armedia.caliente.engine.local.exporter.LocalQueryService.Query;
import com.armedia.caliente.engine.local.exporter.LocalQueryService.Search;
import com.armedia.caliente.engine.local.xml.LocalQueries;
import com.armedia.caliente.engine.local.xml.LocalQueryDataSource;
import com.armedia.caliente.engine.local.xml.LocalQueryPostProcessor;
import com.armedia.caliente.engine.local.xml.LocalQuerySearch;
import com.armedia.caliente.engine.local.xml.LocalQuerySql;
import com.armedia.caliente.engine.local.xml.LocalQueryVersionList;
import com.armedia.commons.utilities.function.CheckedBiConsumer;
import com.armedia.commons.utilities.function.CheckedConsumer;

public class LocalQueryServiceTest {

	public static class LQPPTest implements LocalQueryService.Processor {
		@Override
		public String process(String value) throws Exception {
			return LocalQueryServiceTest.lqppTestProcess(value);
		}
	}

	private static String lqppTestProcess(String value) {
		return "<<" + value + ">>";
	}

	public static String testProcess(String value) {
		return "[[" + value + "]]";
	}

	public static void badMethod(String value, String another) {
		// do nothing
	}

	private static final Object[][] NO_PARAMS = {};
	private static final ResultSetHandler<Object> NO_HANDLER = (rs) -> null;

	private LocalQueryService buildEmptyService() throws Exception {
		final LocalQueryDataSource lqds = new LocalQueryDataSource();
		final LocalQuerySearch lqs = new LocalQuerySearch();
		final LocalQueryPostProcessor lqpp = new LocalQueryPostProcessor();
		final LocalQueries lq = new LocalQueries();

		lq.setRootPath(System.getProperty("user.dir"));

		lqds.setName("dataSource");
		lqds.setUrl("jdbc:h2:mem:test-" + UUID.randomUUID());
		lqds.setDriver("org.h2.Driver");
		lq.getDataSourceDefinitions().add(lqds);

		lqs.setId("search");
		lqs.setDataSource(lqds.getName());
		lqs.setSql("select 1");

		lqpp.setType("jexl3");
		lqpp.setValue("return path;");
		lqs.getPostProcessors().add(lqpp);

		lq.getSearches().add(lqs);

		return new LocalQueryService(lq);
	}

	@Test
	public void testPostProcess() throws Exception {
		LocalQueryService srv = buildEmptyService();
		LocalQueryPostProcessor lqpp = new LocalQueryPostProcessor();

		// First things first: try with nothing, ensure it explodes
		Assertions.assertThrows(IllegalArgumentException.class, () -> srv.buildProcessor(lqpp));

		// Next, ensure it returns the same value from the given script
		lqpp.setType("jexl3");
		for (int i = 0; i < 10; i++) {
			String expected = "Value-" + i;
			lqpp.setValue("return 'Value-' + path;");
			Processor p = srv.buildProcessor(lqpp);
			Assertions.assertEquals(expected, p.process(String.valueOf(i)));
		}

		lqpp.setType("class");

		// First, just the classname
		lqpp.setValue(LQPPTest.class.getName());
		for (int i = 0; i < 10; i++) {
			String path = String.format("path-number-%02d", i);
			Processor p = srv.buildProcessor(lqpp);
			Assertions.assertEquals(LocalQueryServiceTest.lqppTestProcess(path), p.process(path));
		}

		// Next, className & method
		lqpp.setValue(getClass().getName() + "::testProcess");
		for (int i = 0; i < 10; i++) {
			String path = String.format("path-number-%02d", i);
			Processor p = srv.buildProcessor(lqpp);
			Assertions.assertEquals(LocalQueryServiceTest.testProcess(path), p.process(path));
		}

		// a class that doesn't exist
		lqpp.setValue("this.class.does.not.exist.ever.default.for.while.Class");
		Assertions.assertThrows(ClassNotFoundException.class, () -> srv.buildProcessor(lqpp).process("kaka"));

		// bad class syntaxes
		lqpp.setValue("this classname is invalid");
		Assertions.assertThrows(IllegalArgumentException.class, () -> srv.buildProcessor(lqpp).process("kaka"));
		lqpp.setValue("com.some.class.Name:");
		Assertions.assertThrows(IllegalArgumentException.class, () -> srv.buildProcessor(lqpp).process("kaka"));
		lqpp.setValue("com.some.class.Name::");
		Assertions.assertThrows(IllegalArgumentException.class, () -> srv.buildProcessor(lqpp).process("kaka"));
		lqpp.setValue("");
		Assertions.assertThrows(IllegalArgumentException.class, () -> srv.buildProcessor(lqpp).process("kaka"));

		// a class that doesn't implement Processor
		lqpp.setValue("java.lang.Object");
		Assertions.assertThrows(ClassCastException.class, () -> srv.buildProcessor(lqpp).process("kaka"));

		// a method that doesn't exist
		lqpp.setValue("java.lang.Object::process");
		Assertions.assertThrows(NoSuchMethodException.class, () -> srv.buildProcessor(lqpp).process("kaka"));

		// a method that has the wrong signature
		lqpp.setValue(getClass().getName() + "::badMethod");
		Assertions.assertThrows(NoSuchMethodException.class, () -> srv.buildProcessor(lqpp).process("kaka"));
	}

	@SafeVarargs
	private final BasicDataSource buildDataSource(String name,
		CheckedConsumer<QueryRunner, ? extends Exception>... initializers) throws Exception {
		BasicDataSource bds = new BasicDataSource();
		if (name == null) {
			name = "testDataSource-" + UUID.randomUUID().toString();
		}
		bds.setUrl("jdbc:h2:mem:" + name);
		bds.setDriverClassName("org.h2.Driver");

		if (initializers.length > 0) {
			final QueryRunner qr = new QueryRunner(bds);
			for (CheckedConsumer<QueryRunner, ? extends Exception> i : initializers) {
				i.accept(qr);
			}
		}
		return bds;
	}

	@SafeVarargs
	private final BasicDataSource buildDataSource(CheckedConsumer<QueryRunner, ? extends Exception>... initializers)
		throws Exception {
		return buildDataSource(null, initializers);
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
		Object[][] paramsArray = params.toArray(LocalQueryServiceTest.NO_PARAMS);
		qr.insertBatch("insert into paths_one (path) values (?)", LocalQueryServiceTest.NO_HANDLER, paramsArray);
	}

	// paths-2 requires relativization
	private void renderSecondPaths(QueryRunner qr) throws Exception {
		qr.update("create table paths_two ( one varchar(2048), two varchar(2048), three varchar(2048) )");
		List<Object[]> params = new ArrayList<>();
		final AtomicInteger counter = new AtomicInteger(0);
		loadPaths("paths-2.txt").forEach((p) -> {
			String[] data = new String[3];
			data[counter.getAndIncrement() % 3] = p.toString();
			params.add(data);
		});
		Object[][] paramsArray = params.toArray(LocalQueryServiceTest.NO_PARAMS);
		qr.insertBatch("insert into paths_two (one, two, three) values (?, ?, ?)", LocalQueryServiceTest.NO_HANDLER,
			paramsArray);
	}

	// paths-3 requires relativization
	private void renderThirdPaths(QueryRunner qr) throws Exception {
		qr.update("create table paths_three ( path varchar(2048) )");
		List<Object[]> params = new ArrayList<>();
		loadPaths("paths-3.txt").forEach((p) -> {
			params.add(new String[] {
				p.toString()
			});
		});
		Object[][] paramsArray = params.toArray(LocalQueryServiceTest.NO_PARAMS);
		qr.insertBatch("insert into paths_three (path) values (?)", LocalQueryServiceTest.NO_HANDLER, paramsArray);
	}

	@Test
	public void testBuildSearch() throws Exception {
		final LocalQueryService lqs = buildEmptyService();
		final DataSource mockDs = EasyMock.createStrictMock(DataSource.class);
		final LocalQuerySearch lq = new LocalQuerySearch();
		final Function<String, DataSource> nullFinder = (str) -> null;
		final Function<String, DataSource> mockFinder = (str) -> mockDs;

		// First things first: what happens with null arguments?
		Assertions.assertThrows(NullPointerException.class, () -> lqs.buildSearch(null, nullFinder));
		Assertions.assertThrows(NullPointerException.class, () -> lqs.buildSearch(lq, nullFinder));

		@SuppressWarnings("resource")
		Stream<Path> s = null;

		lq.setDataSource("dataSource");
		Assertions.assertThrows(SQLException.class, () -> lqs.buildSearch(lq, mockFinder));

		lq.getPathColumns().add("1");

		Assertions.assertThrows(SQLException.class, () -> lqs.buildSearch(lq, mockFinder));

		lq.setSql("select 1");
		Assertions.assertNotNull(s = lqs.buildSearch(lq, mockFinder).build());
		s.close();

		lq.setSkip(null);
		Assertions.assertNotNull(s = lqs.buildSearch(lq, mockFinder).build());
		s.close();
		lq.setSkip(-1);
		Assertions.assertNotNull(s = lqs.buildSearch(lq, mockFinder).build());
		s.close();
		lq.setSkip(0);
		Assertions.assertNotNull(s = lqs.buildSearch(lq, mockFinder).build());
		s.close();
		lq.setSkip(1);
		Assertions.assertNotNull(s = lqs.buildSearch(lq, mockFinder).build());
		s.close();

		lq.setCount(null);
		Assertions.assertNotNull(s = lqs.buildSearch(lq, mockFinder).build());
		s.close();
		lq.setCount(-1);
		Assertions.assertNotNull(s = lqs.buildSearch(lq, mockFinder).build());
		s.close();
		lq.setCount(0);
		Assertions.assertNotNull(s = lqs.buildSearch(lq, mockFinder).build());
		s.close();
		lq.setCount(1);
		Assertions.assertNotNull(s = lqs.buildSearch(lq, mockFinder).build());
		s.close();
	}

	@Test
	public void testSearches() throws Exception {

		final Set<Path> baseLinesOne = loadPaths("paths-1.txt").collect(Collectors.toCollection(LinkedHashSet::new));
		final Set<Path> baseLinesTwo = loadPaths("paths-2.txt").collect(Collectors.toCollection(LinkedHashSet::new));
		final Set<Path> baseLinesThree = loadPaths("paths-3.txt").collect(Collectors.toCollection(LinkedHashSet::new));

		final Set<Path> lines = new HashSet<>();
		final Set<Path> found = new LinkedHashSet<>();

		try (BasicDataSource dataSource = buildDataSource(this::renderFirstPaths)) {
			try (final LocalQueryService srv = buildEmptyService()) {
				final LocalQuerySearch lqs = new LocalQuerySearch();

				lqs.setSkip(0);
				lqs.setCount(-1);

				lqs.setId("someId");
				lqs.setDataSource("someDataSource");

				lqs.getPathColumns().clear();
				lqs.getPathColumns().add("1");
				lqs.getPathColumns().add("path");
				lqs.getPathColumns().add("-13");
				lqs.getPathColumns().add("13");
				lqs.getPathColumns().add("garbage");

				lqs.setSql("select path from paths_one");

				Search search = srv.buildSearch(lqs, (str) -> dataSource);

				lines.clear();
				lines.addAll(baseLinesOne);
				found.clear();

				try (Stream<Path> targets = search.build()) {
					targets.forEach((str) -> {
						Assertions.assertTrue(lines.remove(str));
						found.add(str);
					});
					Assertions.assertTrue(lines.isEmpty());
					Assertions.assertEquals(baseLinesOne.size(), found.size());
				}
			}
		}

		try (BasicDataSource dataSource = buildDataSource(this::renderSecondPaths)) {
			try (final LocalQueryService srv = buildEmptyService()) {
				final LocalQuerySearch lqs = new LocalQuerySearch();

				lqs.setSkip(0);
				lqs.setCount(-1);

				lqs.setId("someId");
				lqs.setDataSource("someDataSource");

				lqs.getPathColumns().clear();
				lqs.getPathColumns().add("one");
				lqs.getPathColumns().add("2");
				lqs.getPathColumns().add("three");

				lqs.setSql("select * from paths_two");

				Search search = srv.buildSearch(lqs, (str) -> dataSource);

				lines.clear();
				lines.addAll(baseLinesTwo);
				found.clear();

				try (Stream<Path> targets = search.build()) {
					targets.forEach((str) -> {
						Assertions.assertTrue(lines.remove(str));
						found.add(str);
					});
					Assertions.assertTrue(lines.isEmpty());
					Assertions.assertEquals(baseLinesTwo.size(), found.size());
				}
			}
		}

		try (BasicDataSource dataSource = buildDataSource(this::renderThirdPaths)) {
			try (final LocalQueryService srv = buildEmptyService()) {
				final LocalQuerySearch lqs = new LocalQuerySearch();

				lqs.setSkip(0);
				lqs.setCount(-1);

				lqs.setId("someId");
				lqs.setDataSource("someDataSource");

				lqs.getPathColumns().clear();
				lqs.getPathColumns().add("path");

				lqs.setSql("select * from paths_three");

				Search search = srv.buildSearch(lqs, (str) -> dataSource);

				lines.clear();
				lines.addAll(baseLinesThree);
				found.clear();

				try (Stream<Path> targets = search.build()) {
					targets.forEach((str) -> {
						Assertions.assertTrue(lines.remove(str));
						found.add(str);
					});
					Assertions.assertTrue(lines.isEmpty());
					Assertions.assertFalse(found.isEmpty());
					Assertions.assertEquals(baseLinesThree.size(), found.size());
					Assertions.assertEquals(baseLinesThree, found);
				}
			}
		}

		try (BasicDataSource dataSource = buildDataSource(this::renderFirstPaths)) {
			try (final LocalQueryService srv = buildEmptyService()) {
				final LocalQuerySearch lqs = new LocalQuerySearch();

				LocalQueryPostProcessor lqpp = new LocalQueryPostProcessor();
				lqpp.setType("jexl3");
				lqpp.setValue("return path");

				lqs.setSkip(0);
				lqs.setCount(-1);

				lqs.setId("someId");
				lqs.setDataSource("someDataSource");

				lqs.getPathColumns().clear();
				lqs.getPathColumns().add("path");

				lqs.getPostProcessors().clear();
				lqs.getPostProcessors().add(lqpp);

				lqs.setSql("select * from paths_one");

				Search search = srv.buildSearch(lqs, (str) -> dataSource);

				lines.clear();
				lines.addAll(baseLinesOne);
				found.clear();

				try (Stream<Path> targets = search.build()) {
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

	@Test
	public void testBuildDataSource() throws Exception {
		final LocalQueryService srv = buildEmptyService();

		// Create a test in-memory database to which we'll attach the driver
		final LocalQueryDataSource lqds = new LocalQueryDataSource();
		String url = "jdbc:h2:mem:" + UUID.randomUUID().toString();
		String driver = "org.h2.Driver";

		lqds.setUrl(url);
		lqds.setDriver(driver);

		DataSource ds = srv.buildDataSource(lqds);
		Assertions.assertNotNull(ds);
		try (Connection c = ds.getConnection()) {
			Assertions.assertNotNull(c);
		}
		if (AutoCloseable.class.isInstance(ds)) {
			AutoCloseable.class.cast(ds).close();
		}

		lqds.setDriver("some.weird.driver.class");
		lqds.setUrl("jdbc:weird:driver");
		Assertions.assertThrows(SQLException.class, () -> srv.buildDataSource(lqds));

		lqds.setUrl(null);
		Assertions.assertThrows(SQLException.class, () -> srv.buildDataSource(lqds));
	}

	private void renderHistoryIds(QueryRunner qr) throws Exception {
		qr.update(
			"create table history ( object_id varchar(64) not null, history_id varchar(64) not null, primary key (object_id) )");
		List<Object[]> params = new ArrayList<>();
		loadPaths("paths-2.txt").forEach((p) -> {
			params.add(new String[] {
				LocalCommon.calculateId(p), DigestUtils.md5Hex(p.toString())
			});
		});
		Object[][] paramsArray = params.toArray(LocalQueryServiceTest.NO_PARAMS);
		qr.insertBatch("insert into history (object_id, history_id) values (?, ?)", LocalQueryServiceTest.NO_HANDLER,
			paramsArray);
	}

	@Test
	public void testHistoryIds() throws Exception {
		final LocalQueryService srv = buildEmptyService();

		try (BasicDataSource dataSource = buildDataSource(this::renderHistoryIds)) {
			final LocalQuerySql lqs = new LocalQuerySql();

			Assertions.assertThrows(NullPointerException.class, () -> srv.buildHistoryIdQuery(lqs, (str) -> null));
			Assertions.assertThrows(NullPointerException.class,
				() -> srv.buildHistoryIdQuery(lqs, (str) -> dataSource));

			lqs.setId("garbage");
			lqs.setDataSource("garbage");
			lqs.setSql("select history_id from history where object_id = ?");

			final Query<String> q = srv.buildHistoryIdQuery(lqs, (str) -> dataSource);

			loadPaths("paths-2.txt").forEach((p) -> {
				try {
					Assertions.assertEquals(DigestUtils.md5Hex(p.toString()), q.run(LocalCommon.calculateId(p)),
						"Failed while checking [" + p + "]");
				} catch (SQLException e) {
					Assertions.fail("Failed to fetch the history ID for " + p, e);
				}
			});

			for (int i = 0; i < 100; i++) {
				Assertions.assertNull(q.run("bad-id-" + i));
			}
		}

	}

	private int renderVersionLists(QueryRunner qr) throws Exception {
		qr.update(
			"create table versions ( history_id int not null, version_label varchar(64) not null, path varchar(2048) not null, primary key (history_id, version_label) )");
		List<Object[]> params = new ArrayList<>();
		AtomicInteger counter = new AtomicInteger(0);
		AtomicInteger historyId = new AtomicInteger(-1);
		AtomicInteger versionNumber = new AtomicInteger(0);

		final int[] offsets = {
			0, 1, 1, 2, 2, 2, 3, 3, 3, 3
		};
		final boolean[] switches = {
			true, true, false, true, false, false, true, false, false, false
		};

		loadPaths("paths-2.txt").forEach((p) -> {
			final int pos = counter.getAndIncrement();
			final int mod = pos % 10;
			final int off = offsets[mod];
			final int hid = ((pos / 10) * 4) + off;
			historyId.set(hid);
			if (switches[mod]) {
				// New history ID...
				versionNumber.set(0);
			}

			params.add(new Object[] {
				hid, //
				String.format("%d.0", versionNumber.incrementAndGet()), //
				p.toString()
			});
		});
		Object[][] paramsArray = params.toArray(LocalQueryServiceTest.NO_PARAMS);
		qr.insertBatch("insert into versions (history_id, version_label, path) values (?, ?, ?)",
			LocalQueryServiceTest.NO_HANDLER, paramsArray);
		return historyId.get();
	}

	@Test
	public void testVersionLists() throws Exception {
		final LocalQueryService srv = buildEmptyService();
		final AtomicInteger maxHistoryId = new AtomicInteger(-1);
		try (BasicDataSource dataSource = buildDataSource((qr) -> maxHistoryId.set(renderVersionLists(qr)))) {
			final LocalQueryVersionList lqvl = new LocalQueryVersionList();

			Assertions.assertThrows(NullPointerException.class, () -> srv.buildVersionsListQuery(lqvl, (str) -> null));
			Assertions.assertThrows(NullPointerException.class,
				() -> srv.buildVersionsListQuery(lqvl, (str) -> dataSource));

			lqvl.setId("garbage");
			lqvl.setDataSource("garbage");
			lqvl.setSql("select version_label, path from versions where history_id = ? order by version_label");

			final Query<List<Pair<String, Path>>> q = srv.buildVersionsListQuery(lqvl, (str) -> dataSource);

			for (int i = 0; i < maxHistoryId.get(); i++) {
				List<Pair<String, Path>> versions = q.run(i);
				Assertions.assertEquals((i % 4) + 1, versions.size());
			}
			for (int i = 1; i <= 10; i++) {
				List<Pair<String, Path>> versions = q.run(i + maxHistoryId.get());
				Assertions.assertNotNull(versions);
				Assertions.assertTrue(versions.isEmpty());
			}
		}
	}

	private int renderXmlDB(QueryRunner qr) throws Exception {
		qr.update(
			"create table fsobject ( object_id varchar(64) not null, path varchar(2048) not null, history_id varchar(64) not null, version_label varchar(64) not null, primary key (object_id), unique index (path), unique index (history_id, version_label) )");
		List<Object[]> params = new ArrayList<>();
		AtomicInteger counter = new AtomicInteger(0);
		AtomicInteger historyId = new AtomicInteger(-1);
		AtomicInteger versionNumber = new AtomicInteger(0);

		final int[] offsets = {
			0, 1, 1, 2, 2, 2, 3, 3, 3, 3
		};
		final boolean[] switches = {
			true, true, false, true, false, false, true, false, false, false
		};

		loadPaths("paths-2.txt").forEach((p) -> {
			final int pos = counter.getAndIncrement();
			final int mod = pos % 10;
			final int off = offsets[mod];
			final int hid = ((pos / 10) * 4) + off;
			historyId.set(hid);
			if (switches[mod]) {
				// New history ID...
				versionNumber.set(0);
			}

			params.add(new Object[] {
				LocalCommon.calculateId(p), //
				p.toString(), //
				String.format("%08x", hid), //
				String.format("%d.0", versionNumber.incrementAndGet()) //
			});
		});
		Object[][] paramsArray = params.toArray(LocalQueryServiceTest.NO_PARAMS);
		qr.insertBatch("insert into fsobject (object_id, path, history_id, version_label) values (?, ?, ?, ?)",
			LocalQueryServiceTest.NO_HANDLER, paramsArray);
		return historyId.get();
	}

	public static String processorMethod(String str) {
		Assertions.assertNotNull(str);
		return str;
	}

	@Test
	public void testXml() throws Exception {
		final URL url = Thread.currentThread().getContextClassLoader().getResource("local-queries-test.xml");
		Assertions.assertNotNull(url);

		CheckedBiConsumer<String, DataSource, SQLException> dsInit = (name, ds) -> {
			try (Connection c = ds.getConnection()) {
				// TODO: Initialize a test schema so we can re-test the code...?
			}
		};

		try (LocalQueryService srv = new LocalQueryService(url, dsInit)) {
			final QueryRunner qr = new QueryRunner(srv.getDataSource("xmlds"));
			renderXmlDB(qr);

			final int records = qr.query("select count(*) from fsobject", (rs) -> {
				Assertions.assertTrue(rs.next());
				return Integer.valueOf(rs.getString(1));
			});

			final Pair<String, String> historyIdLimits = qr
				.query("select min(history_id), max(history_id) from fsobject", (rs) -> {
					Assertions.assertTrue(rs.next());
					return Pair.of(rs.getString(1), rs.getString(2));
				});

			final AtomicInteger count = new AtomicInteger();
			final AtomicReference<String> firstHistory = new AtomicReference<>();
			final AtomicReference<String> lastHistory = new AtomicReference<>();

			try (Stream<Path> stream = srv.searchPaths()) {
				stream.forEach((p) -> {
					count.incrementAndGet();
					final String objectId = LocalCommon.calculateId(p);
					try {
						final String historyId = srv.getHistoryId(objectId);
						lastHistory.set(historyId);
						firstHistory.compareAndSet(null, historyId);
						List<Pair<String, Path>> versions = srv.getVersionList(historyId);
						final int hid = Integer.valueOf(historyId, 16);
						int expected = (hid % 4) + 1;
						if (Objects.equals(historyId, historyIdLimits.getRight())) {
							expected = 1;
						}
						Assertions.assertEquals(expected, versions.size(), "Mismatch with history " + historyId);
					} catch (Exception e) {
						Assertions.fail("Failed to get the history ID for [" + p + "] (" + objectId + ")");
					}
				});
			}

			Assertions.assertEquals(records, count.get());
			Assertions.assertEquals(historyIdLimits.getLeft(), firstHistory.get());
			Assertions.assertEquals(historyIdLimits.getRight(), lastHistory.get());
		}
	}
}