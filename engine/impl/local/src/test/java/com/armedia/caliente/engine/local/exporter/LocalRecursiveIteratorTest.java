package com.armedia.caliente.engine.local.exporter;

import java.io.File;
import java.util.Iterator;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.engine.local.common.LocalRoot;

public class LocalRecursiveIteratorTest {

	@BeforeAll
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterAll
	public static void tearDownAfterClass() throws Exception {
	}

	@BeforeEach
	public void setUp() throws Exception {
	}

	@AfterEach
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
			Iterator<ExportTarget> it = new LocalRecursiveIterator(new LocalRoot(f), false);
			while (it.hasNext()) {
				System.out.printf("\t%s%n", it.next());
			}
		}
	}

}
