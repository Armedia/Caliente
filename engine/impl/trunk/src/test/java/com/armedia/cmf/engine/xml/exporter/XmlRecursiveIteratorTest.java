package com.armedia.cmf.engine.xml.exporter;

import java.io.File;
import java.util.Iterator;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.engine.xml.common.XmlRoot;
import com.armedia.cmf.engine.xml.exporter.XmlRecursiveIterator;

public class XmlRecursiveIteratorTest {

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
	public void testLocalRecursiveIterator() throws Throwable {
		File[] files = new File[] {
			new File("/home/diego/graphics"), //
			new File("/home/diego/GDrive"), //
			new File("/home/diego/Dropbox"), //
			new File("/home/diego/dctm-scripts"), //
		};
		for (File f : files) {
			System.out.printf("Running on [%s]%n", f);
			Iterator<ExportTarget> it = new XmlRecursiveIterator(new XmlRoot(f), false);
			while (it.hasNext()) {
				System.out.printf("\t%s%n", it.next());
			}
		}
	}

}
