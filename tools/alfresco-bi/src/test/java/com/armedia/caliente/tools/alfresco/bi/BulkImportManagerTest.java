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
package com.armedia.caliente.tools.alfresco.bi;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.tools.alfresco.bi.xml.ScanIndex;
import com.armedia.caliente.tools.alfresco.bi.xml.ScanIndexItem;
import com.armedia.caliente.tools.alfresco.bi.xml.ScanIndexItemVersion;
import com.armedia.commons.utilities.LazyFormatter;
import com.armedia.commons.utilities.ResourceLoader;
import com.armedia.commons.utilities.xml.XmlTools;

public class BulkImportManagerTest {

	private static Path BULK_IMPORT_ROOT = Paths.get("alfresco-bulk-import");
	private static Path MODEL_DIRECTORY = BulkImportManagerTest.BULK_IMPORT_ROOT.resolve("content-models");
	private static Path MANIFEST = BulkImportManagerTest.BULK_IMPORT_ROOT.resolve("CALIENTE_INGESTION_INDEX.txt");

	private static Path FILE_INDEX = BulkImportManagerTest.BULK_IMPORT_ROOT.resolve("scan.files.xml");
	private static Path FOLDER_INDEX = BulkImportManagerTest.BULK_IMPORT_ROOT.resolve("scan.folders.xml");

	private final Path CWD;

	private final Random rand = new Random(System.nanoTime());

	BulkImportManagerTest() throws IOException {
		this.CWD = Paths.get(".").toRealPath();
	}

	private Path buildRandomPath() {
		Path p = null;
		int depth = this.rand.nextInt(10) + 1;
		for (int i = 0; i < depth; i++) {
			if (p == null) {
				p = Paths.get(UUID.randomUUID().toString());
			} else {
				p = p.resolve(UUID.randomUUID().toString());
			}
		}
		return p;
	}

	@Test
	public void testBulkImportManager() throws Exception {
		BulkImportManager bim = null;
		Path p = null;

		Assertions.assertThrows(NullPointerException.class, () -> new BulkImportManager(null));
		Assertions.assertThrows(NullPointerException.class, () -> new BulkImportManager(null, (String) null));
		Assertions.assertThrows(NullPointerException.class, () -> new BulkImportManager(null, (Path) null));
		Assertions.assertThrows(NullPointerException.class, () -> new BulkImportManager(null, null, null));

		p = this.CWD;
		bim = new BulkImportManager(p);
		Assertions.assertEquals(p, bim.getBasePath());
		bim = new BulkImportManager(p, (Path) null);
		Assertions.assertEquals(p, bim.getBasePath());
		bim = new BulkImportManager(p, (String) null);
		Assertions.assertEquals(p, bim.getBasePath());
		bim = new BulkImportManager(p, null, null);
		Assertions.assertEquals(p, bim.getBasePath());
	}

	@Test
	public void testGetContentPathScanIndexItemVersion() {
	}

	@Test
	public void testGetContentPathPath() {
	}

	@Test
	public void testGetArtificialFolderPath() {
	}

	@Test
	public void testGetMetadataPathScanIndexItemVersion() {
	}

	@Test
	public void testGetMetadataPathPath() {
	}

	@Test
	public void testGetBulkImportRoot() {
	}

	@Test
	public void testGetIndexFilePath() {
	}

	@Test
	public void testGetManifestPath() {
	}

	@Test
	public void testOpenManifestWriterCharsetBoolean() {
	}

	@Test
	public void testOpenManifestWriterBoolean() {
	}

	@Test
	public void testOpenManifestWriterCharset() {
	}

	@Test
	public void testOpenManifestReaderCharset() {
	}

	@Test
	public void testOpenManifestReader() {
	}

	@Test
	public void testVerifyPaths() throws Exception {
		BulkImportManager bim = null;
		// Generate a few random paths
		List<Path> paths = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			paths.add(buildRandomPath());
		}

		for (Path p : paths) {
			bim = new BulkImportManager(p);
			p = p.toRealPath();

			Path other = bim.getBasePath();
			Assertions.assertEquals(p, other);

			other = bim.getContentModelsPath();
			Assertions.assertTrue(other.startsWith(p));
			Assertions.assertTrue(other.endsWith(BulkImportManagerTest.MODEL_DIRECTORY));
			Assertions.assertEquals(p.resolve(BulkImportManagerTest.MODEL_DIRECTORY), other);

			other = bim.getManifestPath(false);
			Assertions.assertTrue(other.startsWith(p));
			Assertions.assertTrue(other.endsWith(BulkImportManagerTest.MANIFEST));
			Assertions.assertEquals(p.resolve(BulkImportManagerTest.MANIFEST), other);

			other = bim.getManifestPath(true);
			Assertions.assertFalse(other.startsWith(p));
			Assertions.assertTrue(other.endsWith(BulkImportManagerTest.MANIFEST));
			Assertions.assertNotEquals(p.resolve(BulkImportManagerTest.MANIFEST), other);
			Assertions.assertFalse(other.isAbsolute());

			other = bim.getIndexFilePath(true);
			Assertions.assertTrue(other.startsWith(p));
			Assertions.assertTrue(other.endsWith(BulkImportManagerTest.FOLDER_INDEX));
			Assertions.assertEquals(p.resolve(BulkImportManagerTest.FOLDER_INDEX), other);

			other = bim.getIndexFilePath(false);
			Assertions.assertTrue(other.startsWith(p));
			Assertions.assertTrue(other.endsWith(BulkImportManagerTest.FILE_INDEX));
			Assertions.assertEquals(p.resolve(BulkImportManagerTest.FILE_INDEX), other);

			other = bim.getArtificialFolderPath("abc");
		}
	}

	@Test
	public void testScanAndMarshal() throws Exception {

		// Find the XML file(s)
		Unmarshaller u = XmlTools.getUnmarshaller(ScanIndex.class, ScanIndexItem.class, ScanIndexItemVersion.class);
		Marshaller m = XmlTools.getMarshaller(ScanIndex.class, ScanIndexItem.class, ScanIndexItemVersion.class);
		URL url = ResourceLoader.getResourceOrFile("classpath:/alfresco-bulk-import/scan.folders.xml");
		File f = new File(url.toURI());
		final Path root = f.getParentFile().getParentFile().toPath();
		final BulkImportManager bim = new BulkImportManager(root, root.resolve("streams"));
		final Path biRoot = bim.getBulkImportRoot();
		AtomicLong counter = new AtomicLong(0);
		ScanIndex index = new ScanIndex();
		try (Stream<ScanIndexItem> items = bim.scanItems(true)) {
			items.forEach((item) -> {
				Assertions.assertTrue(item.isDirectory());

				Assertions.assertNotNull(item.getSourceName());
				Assertions.assertTrue(StringUtils.isNotBlank(item.getSourceName()));
				Assertions.assertNotNull(item.getSourcePath());

				Assertions.assertNotNull(item.getTargetName());
				Assertions.assertTrue(StringUtils.isNotBlank(item.getTargetName()));
				Assertions.assertNotNull(item.getTargetPath());

				Assertions.assertFalse(item.getVersions().isEmpty());
				Assertions.assertEquals(1, item.getVersions().size());
				for (ScanIndexItemVersion v : item.getVersions()) {
					Assertions.assertNotNull(v.getNumber());
					Assertions.assertNotNull(v.getContent());
					Path contentPath = bim.resolveContentPath(v);
					Assertions.assertTrue(contentPath.startsWith(root));
					Assertions.assertEquals(v.getContent(),
						FilenameUtils.separatorsToUnix(root.relativize(contentPath).toString()));

					Assertions.assertTrue(StringUtils.isNotBlank(v.getMetadata()));
					Path metadataPath = bim.resolveMetadataPath(v);
					Assertions.assertTrue(metadataPath.startsWith(root));
					Assertions.assertTrue(metadataPath.startsWith(biRoot));
					Path absolute = bim.calculateMetadataPath(contentPath);
					Path relative = bim.getBasePath().relativize(absolute);
					Assertions.assertTrue(metadataPath.endsWith(relative),
						LazyFormatter.lazyFormat("[%s] vs. [%s]", metadataPath, relative));
					Assertions.assertTrue(metadataPath.startsWith(bim.getBasePath()),
						LazyFormatter.lazyFormat("[%s] vs. [%s]", metadataPath, bim.getBasePath()));
					Assertions.assertEquals(bim.getBasePath().resolve(relative), metadataPath);
					Assertions.assertEquals(v.getMetadata(), FilenameUtils.separatorsToUnix(relative.toString()));
				}
				index.getItems().add(item);
				try {
					StringWriter w = new StringWriter();
					m.marshal(item, w);
					Object item2 = u.unmarshal(new StringReader(w.toString()));
					Assertions.assertNotNull(item2);
					Assertions.assertEquals(item, item2);
				} catch (Exception e) {
					Assertions.fail("Failed to marshal an index item");
				}
				counter.incrementAndGet();
			});
		}
		Assertions.assertNotEquals(0, counter.get());
		{
			StringWriter w = new StringWriter();
			m.marshal(index, w);
			Object index2 = u.unmarshal(new StringReader(w.toString()));
			Assertions.assertNotNull(index2);
			Assertions.assertEquals(index, index2);
		}

		counter.set(0);
		Set<BigDecimal> versionNumbers = new HashSet<>();
		index.getItems().clear();
		try (Stream<ScanIndexItem> items = bim.scanItems(false)) {
			items.forEach((item) -> {
				Assertions.assertFalse(item.isDirectory());

				Assertions.assertNotNull(item.getSourceName());
				Assertions.assertTrue(StringUtils.isNotBlank(item.getSourceName()));
				Assertions.assertNotNull(item.getSourcePath());

				Assertions.assertNotNull(item.getTargetName());
				Assertions.assertTrue(StringUtils.isNotBlank(item.getTargetName()));
				Assertions.assertNotNull(item.getTargetPath());

				Assertions.assertFalse(item.getVersions().isEmpty());
				versionNumbers.clear();
				for (ScanIndexItemVersion v : item.getVersions()) {
					Assertions.assertNotNull(v.getNumber());
					Assertions.assertTrue(versionNumbers.add(v.getNumber()));
					Assertions.assertNotNull(v.getContent());
					Path contentPath = bim.resolveContentPath(v);
					Assertions.assertTrue(contentPath.startsWith(root));
					Assertions.assertEquals(v.getContent(),
						FilenameUtils.separatorsToUnix(root.relativize(contentPath).toString()));

					if (v.getMetadata() != null) {
						Assertions.assertTrue(StringUtils.isNotBlank(v.getMetadata()));
						Path metadataPath = bim.resolveMetadataPath(v);
						Assertions.assertTrue(metadataPath.startsWith(root));
						Assertions.assertTrue(metadataPath.startsWith(biRoot));
						Path absolute = bim.calculateMetadataPath(contentPath);
						Path relative = bim.getBasePath().relativize(absolute);
						Assertions.assertTrue(metadataPath.endsWith(relative),
							LazyFormatter.lazyFormat("[%s] vs. [%s]", metadataPath, relative));
						Assertions.assertTrue(metadataPath.startsWith(bim.getBasePath()),
							LazyFormatter.lazyFormat("[%s] vs. [%s]", metadataPath, bim.getBasePath()));
						Assertions.assertEquals(bim.getBasePath().resolve(relative), metadataPath);
						Assertions.assertEquals(v.getMetadata(), FilenameUtils.separatorsToUnix(relative.toString()));
					}
				}
				index.getItems().add(item);
				try {
					StringWriter w = new StringWriter();
					m.marshal(item, w);
					Object item2 = u.unmarshal(new StringReader(w.toString()));
					Assertions.assertNotNull(item2);
					Assertions.assertEquals(item, item2);
				} catch (Exception e) {
					Assertions.fail("Failed to marshal an index item");
				}
				counter.incrementAndGet();
			});
		}
		Assertions.assertNotEquals(0, counter.get());
		{
			StringWriter w = new StringWriter();
			m.marshal(index, w);
			Object index2 = u.unmarshal(new StringReader(w.toString()));
			Assertions.assertNotNull(index2);
			Assertions.assertEquals(index, index2);
		}
	}

}
