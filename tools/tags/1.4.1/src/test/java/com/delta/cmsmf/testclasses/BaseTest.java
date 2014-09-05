package com.delta.cmsmf.testclasses;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.delta.cmsmf.properties.PropertiesManager;

public abstract class BaseTest {

	@BeforeClass
	public void beforeClass() {
		PropertiesManager.close();
	}

	@AfterClass
	public void afterClass() {
		PropertiesManager.close();
	}
}