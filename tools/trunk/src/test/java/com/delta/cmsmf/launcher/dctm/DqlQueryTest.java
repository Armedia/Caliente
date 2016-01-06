package com.delta.cmsmf.launcher.dctm;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class DqlQueryTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testDqlQuery() throws Exception {
		new DqlQuery(
			"                     select                 crap        from   crapola              union select other           from more crap   union    yet more crap to be select on bullshit      order by something");
	}

}
