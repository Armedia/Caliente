package com.armedia.caliente.tools.alfresco.bi;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BulkImportManagerTest {

	private static Path BULK_IMPORT_ROOT = Paths.get("alfresco-bulk-import");
	private static Path MODEL_DIRECTORY = BulkImportManagerTest.BULK_IMPORT_ROOT.resolve("content-models");
	private static Path MANIFEST = BulkImportManagerTest.BULK_IMPORT_ROOT.resolve("CALIENTE_INGESTION_INDEX.txt");

	private static Path FILE_INDEX = BulkImportManagerTest.BULK_IMPORT_ROOT.resolve("scan.files.xml");
	private static Path FOLDER_INDEX = BulkImportManagerTest.BULK_IMPORT_ROOT.resolve("scan.folders.xml");

	private final Path CWD = Paths.get(".").normalize().toAbsolutePath();

	private final Random rand = new Random(System.nanoTime());

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
	void testBulkImportManager() {
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
	void testGetContentPathScanIndexItemVersion() {
		Assertions.fail("Not yet implemented");
	}

	@Test
	void testGetContentPathPath() {
		Assertions.fail("Not yet implemented");
	}

	@Test
	void testGetArtificialFolderPath() {
		Assertions.fail("Not yet implemented");
	}

	@Test
	void testGetMetadataPathScanIndexItemVersion() {
		Assertions.fail("Not yet implemented");
	}

	@Test
	void testGetMetadataPathPath() {
		Assertions.fail("Not yet implemented");
	}

	@Test
	void testGetBulkImportRoot() {
		Assertions.fail("Not yet implemented");
	}

	@Test
	void testGetIndexFilePath() {
		Assertions.fail("Not yet implemented");
	}

	@Test
	void testGetManifestPath() {
		Assertions.fail("Not yet implemented");
	}

	@Test
	void testOpenManifestWriterCharsetBoolean() {
		Assertions.fail("Not yet implemented");
	}

	@Test
	void testOpenManifestWriterBoolean() {
		Assertions.fail("Not yet implemented");
	}

	@Test
	void testOpenManifestWriterCharset() {
		Assertions.fail("Not yet implemented");
	}

	@Test
	void testOpenManifestReaderCharset() {
		Assertions.fail("Not yet implemented");
	}

	@Test
	void testOpenManifestReader() {
		Assertions.fail("Not yet implemented");
	}

	@Test
	void testVerifyPaths() {
		BulkImportManager bim = null;
		// Generate a few random paths
		List<Path> paths = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			paths.add(buildRandomPath());
		}

		for (Path p : paths) {
			bim = new BulkImportManager(p);
			p = p.normalize().toAbsolutePath();

			Path other = bim.getBasePath();
			Assertions.assertEquals(p, other);

			other = bim.getContentModelsPath();
			Assertions.assertTrue(other.startsWith(p));
			Assertions.assertTrue(other.endsWith(BulkImportManagerTest.MODEL_DIRECTORY));
			Assertions.assertEquals(p.resolve(BulkImportManagerTest.MODEL_DIRECTORY), other);

			other = bim.getManifestPath();
			Assertions.assertTrue(other.startsWith(p));
			Assertions.assertTrue(other.endsWith(BulkImportManagerTest.MANIFEST));
			Assertions.assertEquals(p.resolve(BulkImportManagerTest.MANIFEST), other);

			other = bim.getIndexFilePath(true);
			Assertions.assertTrue(other.startsWith(p));
			Assertions.assertTrue(other.endsWith(BulkImportManagerTest.FOLDER_INDEX));
			Assertions.assertEquals(p.resolve(BulkImportManagerTest.FOLDER_INDEX), other);

			other = bim.getIndexFilePath(false);
			Assertions.assertTrue(other.startsWith(p));
			Assertions.assertTrue(other.endsWith(BulkImportManagerTest.FILE_INDEX));
			Assertions.assertEquals(p.resolve(BulkImportManagerTest.FILE_INDEX), other);

			other = bim.getArtificialFolderPath(null);
		}
	}

	@Test
	void testScanItems() {
		Assertions.fail("Not yet implemented");
	}

}
