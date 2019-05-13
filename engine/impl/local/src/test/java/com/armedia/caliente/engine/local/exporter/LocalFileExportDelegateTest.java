package com.armedia.caliente.engine.local.exporter;

import java.io.File;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.engine.local.common.LocalRoot;

class LocalFileExportDelegateTest {

	@Test
	void testCalculateParentTreeIds() throws Exception {
		final File rootFile = FileUtils.getUserDirectory().getCanonicalFile();
		final LocalRoot root = new LocalRoot(rootFile);

		String[] paths = {
			"some/file/in/the/path.txt", "a", "b/c", "d/e/f",
		};

		Set<String> result = null;

		for (String s : paths) {
			result = LocalFileExportDelegate.calculateParentTreeIds(root, new File(rootFile, s));
			Assertions.assertNotNull(result);
		}
	}

}