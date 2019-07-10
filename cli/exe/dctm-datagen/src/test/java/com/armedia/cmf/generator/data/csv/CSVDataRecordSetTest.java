/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.cli.datagen.data.DataRecord;
import com.armedia.caliente.cli.datagen.data.csv.CSVDataRecordSet;

public class CSVDataRecordSetTest {
	private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
	private static File EMPTY = null;
	private static File ONLY_HEADERS = null;
	private static File TEN_RECORDS = null;

	@BeforeAll
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

	@AfterAll
	public static void tearDownAfterClass() throws Exception {
		FileUtils.forceDelete(CSVDataRecordSetTest.EMPTY);
		FileUtils.forceDelete(CSVDataRecordSetTest.ONLY_HEADERS);
		FileUtils.forceDelete(CSVDataRecordSetTest.TEN_RECORDS);
	}

	@BeforeEach
	public void setUp() throws Exception {
	}

	@AfterEach
	public void tearDown() throws Exception {
	}

	@Test
	public void testNodeDataNull() throws Exception {
		final File file = null;
		final URL url = null;
		final Charset charset = null;
		final CSVFormat format = null;

		try (CSVDataRecordSet nd = new CSVDataRecordSet(url)) {
			Assertions.fail("Did not fail with a null URL");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try (CSVDataRecordSet nd = new CSVDataRecordSet(file)) {
			Assertions.fail("Did not fail with a null File");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try (CSVDataRecordSet nd = new CSVDataRecordSet(url, charset)) {
			Assertions.fail("Did not fail with a null URL");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try (CSVDataRecordSet nd = new CSVDataRecordSet(file, charset)) {
			Assertions.fail("Did not fail with a null File");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try (CSVDataRecordSet nd = new CSVDataRecordSet(url, format)) {
			Assertions.fail("Did not fail with a null URL");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try (CSVDataRecordSet nd = new CSVDataRecordSet(file, format)) {
			Assertions.fail("Did not fail with a null File");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try (CSVDataRecordSet nd = new CSVDataRecordSet(url, 0)) {
			Assertions.fail("Did not fail with a null URL");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try (CSVDataRecordSet nd = new CSVDataRecordSet(file, 0)) {
			Assertions.fail("Did not fail with a null File");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try (CSVDataRecordSet nd = new CSVDataRecordSet(url, charset, format)) {
			Assertions.fail("Did not fail with a null URL");
		} catch (IllegalArgumentException e) {
			// All is well
		}
		try (CSVDataRecordSet nd = new CSVDataRecordSet(file, charset, format)) {
			Assertions.fail("Did not fail with a null File");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try (CSVDataRecordSet nd = new CSVDataRecordSet(url, charset, 0)) {
			Assertions.fail("Did not fail with a null URL");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try (CSVDataRecordSet nd = new CSVDataRecordSet(file, charset, 0)) {
			Assertions.fail("Did not fail with a null File");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try (CSVDataRecordSet nd = new CSVDataRecordSet(url, format, 0)) {
			Assertions.fail("Did not fail with a null URL");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try (CSVDataRecordSet nd = new CSVDataRecordSet(file, format, 0)) {
			Assertions.fail("Did not fail with a null File");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try (CSVDataRecordSet nd = new CSVDataRecordSet(url, charset, format, 0)) {
			Assertions.fail("Did not fail with a null URL");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try (CSVDataRecordSet nd = new CSVDataRecordSet(file, charset, format, 0)) {
			Assertions.fail("Did not fail with a null File");
		} catch (IllegalArgumentException e) {
			// All is well
		}

	}

	@Test
	public void testNodeData() throws Exception {

		// First, an empty file
		try (CSVDataRecordSet nd = new CSVDataRecordSet(CSVDataRecordSetTest.EMPTY, 0)) {

		} catch (Exception e) {
			Assertions.assertEquals(Exception.class, e.getClass());
			Assertions.assertTrue(e.getMessage().contains("does not contain a header record"));
		}

		// Next, a file that contains only headers
		;
		try (CSVDataRecordSet nd = new CSVDataRecordSet(CSVDataRecordSetTest.ONLY_HEADERS, 0)) {
			Assertions.assertFalse(nd.hasNext());
		}

		// Next, a file that contains exactly 10 records
		try (CSVDataRecordSet nd = new CSVDataRecordSet(CSVDataRecordSetTest.TEN_RECORDS, 1)) {
			int i = 0;
			for (DataRecord r : nd) {
				Assertions.assertNotNull(r);
				i++;
			}
			Assertions.assertEquals(10, i);
		}

		// Next, a file that contains exactly 10 records, iteratively more times
		for (int l = 1; l < 10; l++) {
			;
			try (CSVDataRecordSet nd = new CSVDataRecordSet(CSVDataRecordSetTest.TEN_RECORDS, l)) {
				int i = 0;
				for (DataRecord r : nd) {
					Assertions.assertNotNull(r);
					i++;
				}
				Assertions.assertEquals(10 * l, i);
			}
		}

		// Finally, an "infinite loop" read 10000 times
		try (CSVDataRecordSet nd = new CSVDataRecordSet(CSVDataRecordSetTest.TEN_RECORDS, 0)) {
			int i = 0;
			for (DataRecord r : nd) {
				Assertions.assertNotNull(r);
				i++;
				if (i == 10000) {
					break;
				}
			}
			Assertions.assertEquals(10000, i);
		}
	}

	@Test
	public void testGetCurrentLoop() throws Exception {
		try (CSVDataRecordSet nd = new CSVDataRecordSet(CSVDataRecordSetTest.TEN_RECORDS, 0)) {
			for (int l = 1; l < 100; l++) {
				int i = 0;
				for (DataRecord r : nd) {
					Assertions.assertNotNull(r);
					i++;
					if (i == 10) {
						// Manually break, so we know when we've looped
						break;
					}
				}
				Assertions.assertEquals(l, nd.getCurrentLoop());
			}
		}
	}

	@Test
	public void testGetRecordNumber() throws Exception {
		try (CSVDataRecordSet nd = new CSVDataRecordSet(CSVDataRecordSetTest.TEN_RECORDS, 10)) {
			int i = 0;
			for (DataRecord r : nd) {
				Assertions.assertNotNull(r);
				i++;
				Assertions.assertEquals(i, nd.getRecordNumber());
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
					Assertions.assertEquals(expected.size(), actual.size());
					Assertions.assertEquals(expected, actual);
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
					Assertions.assertEquals(i, nd.getColumnCount());
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
					Assertions.assertEquals(expected.size(), nd.getColumnCount());
					for (int c = 0; c < expected.size(); c++) {
						Assertions.assertEquals(expected.get(c), nd.getColumnName(c));
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
			Assertions.assertTrue(nd.hasNext());
			Assertions.assertNotNull(nd.next());
			closed = nd;
			closed.close();
		}
		Assertions.assertFalse(closed.hasNext());
		try {
			closed.next();
			Assertions.fail("Did not fail when invoking next() on a closed instance");
		} catch (NoSuchElementException e) {
			// All is well
		}
	}

	@Test
	public void testRemove() throws Exception {
		try (CSVDataRecordSet nd = new CSVDataRecordSet(CSVDataRecordSetTest.TEN_RECORDS, 1)) {
			Assertions.assertTrue(nd.hasNext());
			Assertions.assertNotNull(nd.next());
			try {
				nd.remove();
				Assertions.fail("remove() invocation did not fail");
			} catch (UnsupportedOperationException e) {
				// All is well
			}
		}
	}
}
