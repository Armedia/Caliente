package com.armedia.caliente.engine.local.exporter;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import javax.sql.DataSource;

import org.apache.commons.dbutils.ResultSetHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.engine.local.exporter.LocalQueryService.Processor;
import com.armedia.caliente.engine.local.xml.LocalQueries;
import com.armedia.caliente.engine.local.xml.LocalQueryDataSource;
import com.armedia.caliente.engine.local.xml.LocalQueryPostProcessor;
import com.armedia.caliente.engine.local.xml.LocalQuerySearch;

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

		lqds.setName(UUID.randomUUID().toString());
		lqds.setUrl("jdbc:h2:mem:test-" + UUID.randomUUID());
		lqds.setDriver("org.h2.Driver");
		lq.getDataSourceDefinitions().add(lqds);

		lqs.setId(UUID.randomUUID().toString());
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
		LocalQueryPostProcessor lqpp = new LocalQueryPostProcessor();

		// First things first: try with nothing, ensure it explodes
		Assertions.assertThrows(IllegalArgumentException.class, () -> LocalQueryService.buildProcessor(lqpp));

		// Next, ensure it returns the same value from the given script
		lqpp.setType("jexl3");
		for (int i = 0; i < 10; i++) {
			String expected = "Value-" + i;
			lqpp.setValue("return 'Value-' + path;");
			Processor p = LocalQueryService.buildProcessor(lqpp);
			Assertions.assertEquals(expected, p.process(String.valueOf(i)));
		}

		lqpp.setType("class");

		// First, just the classname
		lqpp.setValue(LQPPTest.class.getName());
		for (int i = 0; i < 10; i++) {
			String path = String.format("path-number-%02d", i);
			Processor p = LocalQueryService.buildProcessor(lqpp);
			Assertions.assertEquals(LocalQueryServiceTest.lqppTestProcess(path), p.process(path));
		}

		// Next, className & method
		lqpp.setValue(getClass().getName() + "::testProcess");
		for (int i = 0; i < 10; i++) {
			String path = String.format("path-number-%02d", i);
			Processor p = LocalQueryService.buildProcessor(lqpp);
			Assertions.assertEquals(LocalQueryServiceTest.testProcess(path), p.process(path));
		}

		// a class that doesn't exist
		lqpp.setValue("this.class.does.not.exist.ever.default.for.while.Class");
		Assertions.assertThrows(ClassNotFoundException.class,
			() -> LocalQueryService.buildProcessor(lqpp).process("kaka"));

		// bad class syntaxes
		lqpp.setValue("this classname is invalid");
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> LocalQueryService.buildProcessor(lqpp).process("kaka"));
		lqpp.setValue("com.some.class.Name:");
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> LocalQueryService.buildProcessor(lqpp).process("kaka"));
		lqpp.setValue("com.some.class.Name::");
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> LocalQueryService.buildProcessor(lqpp).process("kaka"));
		lqpp.setValue("");
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> LocalQueryService.buildProcessor(lqpp).process("kaka"));

		// a class that doesn't implement Processor
		lqpp.setValue("java.lang.Object");
		Assertions.assertThrows(ClassCastException.class, () -> LocalQueryService.buildProcessor(lqpp).process("kaka"));

		// a method that doesn't exist
		lqpp.setValue("java.lang.Object::process");
		Assertions.assertThrows(NoSuchMethodException.class,
			() -> LocalQueryService.buildProcessor(lqpp).process("kaka"));

		// a method that has the wrong signature
		lqpp.setValue(getClass().getName() + "::badMethod");
		Assertions.assertThrows(NoSuchMethodException.class,
			() -> LocalQueryService.buildProcessor(lqpp).process("kaka"));
	}

	/*
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
	public void testStream() throws Exception {
		final LocalQuerySearch lq = new LocalQuerySearch();
	
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
				Assertions.assertTrue(lines.isEmpty());
				Assertions.assertFalse(found.isEmpty());
				Assertions.assertEquals(baseLinesThree.size(), lines.size());
				Assertions.assertEquals(baseLinesThree.size(), found);
			}
		}
	
		try (BasicDataSource dataSource = buildDataSource(this::renderFirstPaths)) {
	
			LocalQueryPostProcessor lqpp = new LocalQueryPostProcessor();
			lqpp.setType("jexl3");
			lqpp.setValue("return path");
	
			lq.setSkip(0);
			lq.setCount(-1);
	
			lq.setId("someId");
			lq.setDataSource("someDataSource");
	
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
	*/

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
}