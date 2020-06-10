package com.armedia.caliente.engine.local.xml;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.engine.local.xml.LocalQueryDataSource.Setting;

public class LocalQueryDataSourceTest {

	@Test
	public void testUrl() {
		final LocalQueryDataSource lqds = new LocalQueryDataSource();
		for (int i = 0; i < 10; i++) {
			String value = String.format("url-%02d", i);

			lqds.setUrl(value);
			Assertions.assertEquals(value, lqds.getUrl());
		}
	}

	@Test
	public void testDriver() {
		final LocalQueryDataSource lqds = new LocalQueryDataSource();
		for (int i = 0; i < 10; i++) {
			String value = String.format("driver-%02d", i);

			lqds.setDriver(value);
			Assertions.assertEquals(value, lqds.getDriver());
		}
	}

	@Test
	public void testUser() {
		final LocalQueryDataSource lqds = new LocalQueryDataSource();
		for (int i = 0; i < 10; i++) {
			String value = String.format("user-%02d", i);

			lqds.setUser(value);
			Assertions.assertEquals(value, lqds.getUser());
		}
	}

	@Test
	public void testPassword() {
		final LocalQueryDataSource lqds = new LocalQueryDataSource();
		for (int i = 0; i < 10; i++) {
			String value = String.format("password-%02d", i);

			lqds.setPassword(value);
			Assertions.assertEquals(value, lqds.getPassword());
		}
	}

	@Test
	public void testName() {
		final LocalQueryDataSource lqds = new LocalQueryDataSource();
		for (int i = 0; i < 10; i++) {
			String value = String.format("name-%02d", i);

			lqds.setName(value);
			Assertions.assertEquals(value, lqds.getName());
		}
	}

	@Test
	public void testSettings() {
		Setting s = new Setting();
		for (int i = 0; i < 10; i++) {
			String name = String.format("name-%02d", i);
			s.setName(name);
			Assertions.assertEquals(name, s.getName());
		}
		for (int i = 0; i < 10; i++) {
			String value = String.format("value-%02d", i);
			s.setValue(value);
			Assertions.assertEquals(value, s.getValue());
		}
	}

	@Test
	public void testSettingsMap() {
		final LocalQueryDataSource lqds = new LocalQueryDataSource();
		final Map<String, String> settings = lqds.getSettings();
		Map<String, String> expected = new LinkedHashMap<>();
		for (int i = 0; i < 100; i++) {
			Setting s = new Setting();
			String name = String.format("setting-%02d", i);
			String value = String.format("value-%02d", i);
			s.setName(name);
			s.setValue(value);
			expected.put("jdbc." + name, value);
			settings.put(name, value);
		}

		Assertions.assertEquals(expected, lqds.buildSettingsMap());
	}

	@Test
	public void getInstance() throws Exception {
		// Create a test in-memory database to which we'll attach the driver
		final LocalQueryDataSource lqds = new LocalQueryDataSource();
		String url = "jdbc:h2:mem:" + UUID.randomUUID().toString();
		String driver = "org.h2.Driver";

		lqds.setUrl(url);
		lqds.setDriver(driver);

		DataSource ds = lqds.getInstance();
		Assertions.assertNotNull(ds);
		try (Connection c = ds.getConnection()) {
			Assertions.assertNotNull(c);
		}
		if (AutoCloseable.class.isInstance(ds)) {
			AutoCloseable.class.cast(ds).close();
		}

		lqds.setDriver("some.weird.driver.class");
		lqds.setUrl("jdbc:weird:driver");
		Assertions.assertThrows(SQLException.class, lqds::getInstance);
	}
}