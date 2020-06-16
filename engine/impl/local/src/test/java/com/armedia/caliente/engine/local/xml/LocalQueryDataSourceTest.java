package com.armedia.caliente.engine.local.xml;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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

		for (int nA = 0; nA < 10; nA++) {
			for (int vA = 0; vA < 10; vA++) {
				Setting a = new Setting("name-" + nA, "value-" + vA);

				Assertions.assertFalse(a.equals(null));
				Assertions.assertTrue(a.equals(a));
				Assertions.assertFalse(a.equals(new Object()));

				for (int nB = 0; nB < 10; nB++) {
					for (int vB = 0; vB < 10; vB++) {
						Setting b = new Setting("name-" + nB, "value-" + vB);

						if ((nA == nB) && (vA == vB)) {
							Assertions.assertEquals(a, b);
							Assertions.assertEquals(a.hashCode(), b.hashCode());
							Assertions.assertEquals(a.toString(), b.toString());
						} else {
							Assertions.assertNotEquals(a, b);
							Assertions.assertNotEquals(a.toString(), b.toString());
							if (nA == nB) {
								Assertions.assertEquals(a.hashCode(), b.hashCode());
							} else {
								Assertions.assertNotEquals(a.hashCode(), b.hashCode());
							}
						}
					}
				}
			}
		}
	}

	@Test
	public void testSettingsMap() {
		final LocalQueryDataSource lqds = new LocalQueryDataSource();
		final Map<String, String> settings = lqds.getSettings();
		Map<String, String> expected = new LinkedHashMap<>();
		for (int i = 0; i < 100; i++) {
			String name = String.format("setting-%02d", i);
			String value = String.format("value-%02d", i);
			expected.put(name, value);
			settings.put(name, value);
		}

		Assertions.assertEquals(expected, lqds.getSettings());
	}

	@Test
	public void testAfterUnmarshal() {
		final LocalQueryDataSource lqds = new LocalQueryDataSource();

		Map<String, String> expected = new TreeMap<>();
		lqds.settings = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			String name = String.format("setting-%02d", i);
			String value = String.format("value-%02d", i);
			lqds.settings.add(new Setting(name, value));
			expected.put(name, value);
		}
		lqds.settings.add(new Setting("", ""));
		lqds.settings.add(new Setting("nullValue", null));
		lqds.settings.add(new Setting(null, "nullName"));
		lqds.settings.add(new Setting(null, null));

		lqds.afterUnmarshal(null, null);
		Assertions.assertEquals(expected, lqds.getSettings());
	}

	@Test
	public void testBeforeMarshal() {
		final LocalQueryDataSource lqds = new LocalQueryDataSource();

		Map<String, String> settings = lqds.getSettings();
		List<Setting> expected = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			String name = String.format("setting-%02d", i);
			String value = String.format("value-%02d", i);
			settings.put(name, value);
			expected.add(new Setting(name, value));
		}

		lqds.beforeMarshal(null);
		Assertions.assertEquals(expected, lqds.settings);
	}
}