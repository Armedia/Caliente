package com.armedia.caliente.tools.datasource.spring;

import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.support.GenericXmlApplicationContext;

import com.armedia.caliente.tools.datasource.spring.SpringDataSourceLocator;
import com.armedia.caliente.tools.datasource.spring.SpringSetting;
import com.armedia.commons.utilities.CfgTools;

public class SpringDataSourceLocatorTest {

	private static final String DSNAME_FMT = "ds%02x";
	private static GenericXmlApplicationContext CONTEXT = null;

	@BeforeAll
	public static void beforeAll() throws Exception {
		// Create initial context
		SpringDataSourceLocatorTest.CONTEXT = new GenericXmlApplicationContext("classpath:test-context.xml");
	}

	@Test
	void testSupportsLocationType() {
		SpringDataSourceLocator loc = new SpringDataSourceLocator();
		Assertions.assertFalse(loc.supportsLocationType(null));
		Assertions.assertTrue(loc.supportsLocationType(SpringDataSourceLocator.SPRING));
		Assertions.assertTrue(loc.supportsLocationType(SpringDataSourceLocator.SPRING.toUpperCase()));
		Assertions.assertTrue(loc.supportsLocationType(SpringDataSourceLocator.SPRING.toLowerCase()));
	}

	@Test
	void testLocateDataSource() throws Exception {
		SpringDataSourceLocator loc = SpringDataSourceLocatorTest.CONTEXT.getBean(SpringDataSourceLocator.class);
		Map<String, String> settings = new TreeMap<>();
		for (int i = 0; i < 10; i++) {
			// If it's even, then it should exist. If not, it shouldn't...
			settings.clear();
			String name = String.format(SpringDataSourceLocatorTest.DSNAME_FMT, i);
			settings.put(SpringSetting.BEAN_NAME.getLabel(), name);

			if ((i % 2) == 0) {
				// Should exist
				Assertions.assertNotNull(loc.locateDataSource(new CfgTools(settings)), name);
			} else {
				// Should not exist
				Assertions.assertThrows(NoSuchBeanDefinitionException.class,
					() -> loc.locateDataSource(new CfgTools(settings)));
			}
		}

		settings.clear();
		settings.put(SpringSetting.BEAN_NAME.getLabel(), "springDataSourceLocator");
		Assertions.assertThrows(BeanNotOfRequiredTypeException.class,
			() -> loc.locateDataSource(new CfgTools(settings)));

		settings.clear();
		Assertions.assertThrows(NullPointerException.class, () -> loc.locateDataSource(new CfgTools(settings)));
	}

	@AfterAll
	public static void afterAll() {
		SpringDataSourceLocatorTest.CONTEXT.close();
		SpringDataSourceLocatorTest.CONTEXT = null;
	}
}