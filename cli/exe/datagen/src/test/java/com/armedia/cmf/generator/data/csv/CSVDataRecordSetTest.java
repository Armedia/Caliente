package com.armedia.cmf.generator.data.csv;

import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.armedia.caliente.cli.datagen.data.DataRecord;
import com.armedia.caliente.cli.datagen.data.csv.CSVDataRecordSet;

public class CSVDataRecordSetTest {
	private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
	private static File EMPTY = null;
	private static File ONLY_HEADERS = null;
	private static File TEN_RECORDS = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		// Create an empty file
		CSVDataRecordSetTest.EMPTY = File
			.createTempFile(String.format("%s-empty-", CSVDataRecordSetTest.class.getCanonicalName()), ".csv");

		// Create 3 headers: a,b,c
		CSVDataRecordSetTest.ONLY_HEADERS = File.createTempFile("NodeDataTest-only-headers-", ".csv");
		StringBuilder b = new StringBuilder();
		try (CSVPrinter p = new CSVPrinter(b, CSVFormat.DEFAULT)) {
			p.print("a");
			p.print("b");
			p.print("c");
			p.println();
		}
		FileUtils.write(CSVDataRecordSetTest.ONLY_HEADERS, b, CSVDataRecordSetTest.DEFAULT_CHARSET);
		b.setLength(0);

		// Create 3 headers: a,b,c, and ten records...
		CSVDataRecordSetTest.TEN_RECORDS = File.createTempFile("NodeDataTest-ten-records-", ".csv");
		try (CSVPrinter p = new CSVPrinter(b, CSVFormat.DEFAULT)) {
			p.print("a");
			p.print("b");
			p.print("c");
			p.println();
			for (int i = 0; i < 10; i++) {
				p.print(String.format("%d", (3 * i) + 1));
				p.print(String.format("%d", (3 * i) + 2));
				p.print(String.format("%d", (3 * i) + 3));
				p.println();
			}
		}
		FileUtils.write(CSVDataRecordSetTest.TEN_RECORDS, b, CSVDataRecordSetTest.DEFAULT_CHARSET);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		FileUtils.forceDelete(CSVDataRecordSetTest.EMPTY);
		FileUtils.forceDelete(CSVDataRecordSetTest.ONLY_HEADERS);
		FileUtils.forceDelete(CSVDataRecordSetTest.TEN_RECORDS);
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testNodeDataNull() throws Exception {
		final File file = null;
		final URL url = null;
		final Charset charset = null;
		final CSVFormat format = null;

		try (CSVDataRecordSet nd = new CSVDataRecordSet(url)) {
			Assert.fail("Did not fail with a null URL");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try (CSVDataRecordSet nd = new CSVDataRecordSet(file)) {
			Assert.fail("Did not fail with a null File");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try (CSVDataRecordSet nd = new CSVDataRecordSet(url, charset)) {
			Assert.fail("Did not fail with a null URL");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try (CSVDataRecordSet nd = new CSVDataRecordSet(file, charset)) {
			Assert.fail("Did not fail with a null File");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try (CSVDataRecordSet nd = new CSVDataRecordSet(url, format)) {
			Assert.fail("Did not fail with a null URL");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try (CSVDataRecordSet nd = new CSVDataRecordSet(file, format)) {
			Assert.fail("Did not fail with a null File");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try (CSVDataRecordSet nd = new CSVDataRecordSet(url, 0)) {
			Assert.fail("Did not fail with a null URL");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try (CSVDataRecordSet nd = new CSVDataRecordSet(file, 0)) {
			Assert.fail("Did not fail with a null File");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try (CSVDataRecordSet nd = new CSVDataRecordSet(url, charset, format)) {
			Assert.fail("Did not fail with a null URL");
		} catch (IllegalArgumentException e) {
			// All is well
		}
		try (CSVDataRecordSet nd = new CSVDataRecordSet(file, charset, format)) {
			Assert.fail("Did not fail with a null File");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try (CSVDataRecordSet nd = new CSVDataRecordSet(url, charset, 0)) {
			Assert.fail("Did not fail with a null URL");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try (CSVDataRecordSet nd = new CSVDataRecordSet(file, charset, 0)) {
			Assert.fail("Did not fail with a null File");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try (CSVDataRecordSet nd = new CSVDataRecordSet(url, format, 0)) {
			Assert.fail("Did not fail with a null URL");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try (CSVDataRecordSet nd = new CSVDataRecordSet(file, format, 0)) {
			Assert.fail("Did not fail with a null File");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try (CSVDataRecordSet nd = new CSVDataRecordSet(url, charset, format, 0)) {
			Assert.fail("Did not fail with a null URL");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try (CSVDataRecordSet nd = new CSVDataRecordSet(file, charset, format, 0)) {
			Assert.fail("Did not fail with a null File");
		} catch (IllegalArgumentException e) {
			// All is well
		}

	}

	@Test
	public void testNodeData() throws Exception {

		// First, an empty file
		try (CSVDataRecordSet nd = new CSVDataRecordSet(CSVDataRecordSetTest.EMPTY, 0)) {

		} catch (Exception e) {
			Assert.assertEquals(Exception.class, e.getClass());
			Assert.assertTrue(e.getMessage().contains("does not contain a header record"));
		}

		// Next, a file that contains only headers
		;
		try (CSVDataRecordSet nd = new CSVDataRecordSet(CSVDataRecordSetTest.ONLY_HEADERS, 0)) {
			Assert.assertFalse(nd.hasNext());
		}

		// Next, a file that contains exactly 10 records
		try (CSVDataRecordSet nd = new CSVDataRecordSet(CSVDataRecordSetTest.TEN_RECORDS, 1)) {
			int i = 0;
			for (DataRecord r : nd) {
				Assert.assertNotNull(r);
				i++;
			}
			Assert.assertEquals(10, i);
		}

		// Next, a file that contains exactly 10 records, iteratively more times
		for (int l = 1; l < 10; l++) {
			;
			try (CSVDataRecordSet nd = new CSVDataRecordSet(CSVDataRecordSetTest.TEN_RECORDS, l)) {
				int i = 0;
				for (DataRecord r : nd) {
					Assert.assertNotNull(r);
					i++;
				}
				Assert.assertEquals(10 * l, i);
			}
		}

		// Finally, an "infinite loop" read 10000 times
		try (CSVDataRecordSet nd = new CSVDataRecordSet(CSVDataRecordSetTest.TEN_RECORDS, 0)) {
			int i = 0;
			for (DataRecord r : nd) {
				Assert.assertNotNull(r);
				i++;
				if (i == 10000) {
					break;
				}
			}
			Assert.assertEquals(10000, i);
		}
	}

	@Test
	public void testGetCurrentLoop() throws Exception {
		try (CSVDataRecordSet nd = new CSVDataRecordSet(CSVDataRecordSetTest.TEN_RECORDS, 0)) {
			for (int l = 1; l < 100; l++) {
				int i = 0;
				for (DataRecord r : nd) {
					Assert.assertNotNull(r);
					i++;
					if (i == 10) {
						// Manually break, so we know when we've looped
						break;
					}
				}
				Assert.assertEquals(l, nd.getCurrentLoop());
			}
		}
	}

	@Test
	public void testGetRecordNumber() throws Exception {
		try (CSVDataRecordSet nd = new CSVDataRecordSet(CSVDataRecordSetTest.TEN_RECORDS, 10)) {
			int i = 0;
			for (DataRecord r : nd) {
				Assert.assertNotNull(r);
				i++;
				Assert.assertEquals(i, nd.getRecordNumber());
			}
		}
	}

	@Test
	public void testGetColumnNames() throws Exception {
		File f = File.createTempFile("csv-test", ".csv");
		f.deleteOnExit();
		StringBuilder b = new StringBuilder();
		try {
			for (int i = 1; i < 256; i++) {
				b.setLength(0);
				Set<String> expected = new LinkedHashSet<>();
				// Print out the headers
				CSVPrinter p = new CSVPrinter(b, CSVFormat.DEFAULT);
				try {
					for (int c = 0; c < i; c++) {
						String v = String.format("COLUMN_%04x_%s", c, UUID.randomUUID().toString());
						expected.add(v);
						p.print(v);
					}
					p.println();
				} finally {
					p.close();
				}
				FileUtils.write(f, b, CSVDataRecordSetTest.DEFAULT_CHARSET);

				try (CSVDataRecordSet nd = new CSVDataRecordSet(f, 1)) {
					Set<String> actual = nd.getColumnNames();
					Assert.assertEquals(expected.size(), actual.size());
					Assert.assertEquals(expected, actual);
				}
			}
		} finally {
			FileUtils.forceDelete(f);
		}
	}

	@Test
	public void testGetColumnCount() throws Exception {
		File f = File.createTempFile("csv-test", ".csv");
		f.deleteOnExit();
		StringBuilder b = new StringBuilder();
		try {
			for (int i = 1; i < 256; i++) {
				// Print out the headers
				b.setLength(0);
				CSVPrinter p = new CSVPrinter(b, CSVFormat.DEFAULT);
				try {
					for (int c = 0; c < i; c++) {
						p.print(String.format("%08x", c));
					}
					p.println();
				} finally {
					p.close();
				}
				FileUtils.write(f, b, CSVDataRecordSetTest.DEFAULT_CHARSET);

				try (CSVDataRecordSet nd = new CSVDataRecordSet(f, 1)) {
					Assert.assertEquals(i, nd.getColumnCount());
				}
			}
		} finally {
			FileUtils.forceDelete(f);
		}
	}

	@Test
	public void testGetColumnName() throws Exception {
		File f = File.createTempFile("csv-test", ".csv");
		f.deleteOnExit();
		StringBuilder b = new StringBuilder();
		try {
			for (int i = 1; i < 256; i++) {
				b.setLength(0);
				List<String> expected = new ArrayList<>(i);
				// Print out the headers
				CSVPrinter p = new CSVPrinter(b, CSVFormat.DEFAULT);
				try {
					for (int c = 0; c < i; c++) {
						String v = String.format("COLUMN_%04x_%s", c, UUID.randomUUID().toString());
						expected.add(v);
						p.print(v);
					}
					p.println();
				} finally {
					p.close();
				}
				FileUtils.write(f, b, CSVDataRecordSetTest.DEFAULT_CHARSET);

				try (CSVDataRecordSet nd = new CSVDataRecordSet(f, 1)) {
					Assert.assertEquals(expected.size(), nd.getColumnCount());
					for (int c = 0; c < expected.size(); c++) {
						Assert.assertEquals(expected.get(c), nd.getColumnName(c));
					}
				}
			}
		} finally {
			FileUtils.forceDelete(f);
		}
	}

	@Test
	public void testClose() throws Exception {
		CSVDataRecordSet closed = null;
		try (CSVDataRecordSet nd = new CSVDataRecordSet(CSVDataRecordSetTest.TEN_RECORDS, 1)) {
			Assert.assertTrue(nd.hasNext());
			Assert.assertNotNull(nd.next());
			closed = nd;
			closed.close();
		}
		Assert.assertFalse(closed.hasNext());
		try {
			closed.next();
			Assert.fail("Did not fail when invoking next() on a closed instance");
		} catch (NoSuchElementException e) {
			// All is well
		}
	}

	@Test
	public void testRemove() throws Exception {
		try (CSVDataRecordSet nd = new CSVDataRecordSet(CSVDataRecordSetTest.TEN_RECORDS, 1)) {
			Assert.assertTrue(nd.hasNext());
			Assert.assertNotNull(nd.next());
			try {
				nd.remove();
				Assert.fail("remove() invocation did not fail");
			} catch (UnsupportedOperationException e) {
				// All is well
			}
		}
	}
}
