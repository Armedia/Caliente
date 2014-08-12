package com.delta.cmsmf.testclasses;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.configuration.ConfigurationException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.delta.cmsmf.constants.CMSMFProperties;
import com.delta.cmsmf.properties.PropertiesManager;
import com.delta.cmsmf.runtime.RunTimeProperties;

public class CheckForRepoOperatorNameAttrListTest {

	@BeforeClass
	public void beforeClass() {
		PropertiesManager.close();
	}

	@AfterClass
	public void afterClass() {
		PropertiesManager.close();
	}

	@Test
	public void test() throws ConfigurationException {
		Properties props = new Properties();
		List<String> expected = new ArrayList<String>();
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < 128; i++) {
			String uuid = UUID.randomUUID().toString();
			if (i > 0) {
				b.append(',');
			}
			b.append(uuid);
			expected.add(uuid);
		}
		props.setProperty(CMSMFProperties.CMSMF_APP_REPOSITORYOWNER_ATTRIBUTESTOCHECK.name, b.toString());

		PropertiesManager.addPropertySource(props);
		PropertiesManager.init();

		List<String> actual = RunTimeProperties.getRunTimePropertiesInstance().getAttrsToCheckForRepoOperatorName();
		Assert.assertEquals(expected, actual);
	}
}