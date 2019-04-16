package com.armedia.caliente.engine.local.common;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

class LocalCommonTest {

	@Test
	void testGetRootDirectory() {
		Assertions.assertThrows(NullPointerException.class, () -> LocalCommon.getRootDirectory(null));
		Map<String, String> settings = new HashMap<>();
		CfgTools cfg = new CfgTools(settings);
		Assertions.assertNull(LocalCommon.getRootDirectory(cfg));

		String path = null;
		File file = null;

		path = "";
		settings.put(LocalSetting.ROOT.getLabel(), path);
		file = LocalCommon.getRootDirectory(cfg);
		Assertions.assertNull(file);

		path = "/some/folder/path";
		settings.put(LocalSetting.ROOT.getLabel(), path);
		file = LocalCommon.getRootDirectory(cfg);
		Assertions.assertEquals(path, file.getPath());

		path = "a/relative/variation/some/folder/path/../another/././../target";
		settings.put(LocalSetting.ROOT.getLabel(), path);
		file = Tools.canonicalize(new File(path));
		path = file.getPath();
		file = LocalCommon.getRootDirectory(cfg);
		Assertions.assertEquals(path, file.getPath());

		path = "/some/folder/path/../another/././../target";
		settings.put(LocalSetting.ROOT.getLabel(), path);
		file = Tools.canonicalize(new File(path));
		path = file.getPath();
		file = LocalCommon.getRootDirectory(cfg);
		Assertions.assertEquals(path, file.getPath());
	}

	@Test
	void testCalculateId() {
		String[] paths = {
			"a", "b", "c/d/../e", "f/g/h/i/j/k/../l", "a/././././b"
		};
		final Path root = Tools.canonicalize(FileUtils.getUserDirectory()).toPath();
		for (String path1 : paths) {
			path1 = FilenameUtils.normalize(path1);
			Assertions.assertNotNull(path1,
				String.format("The path [%s] is a bad test example - fix the unit test", path1));
			Path child1 = root.resolve(path1).normalize();
			child1 = root.relativize(child1);
			String portable1 = LocalCommon.getPortablePath(child1.toString());

			for (String path2 : paths) {
				path2 = FilenameUtils.normalize(path2);
				Assertions.assertNotNull(path2,
					String.format("The path [%s] is a bad test example - fix the unit test", path2));
				Path child2 = root.resolve(path2).normalize();
				child2 = root.relativize(child2);
				String portable2 = LocalCommon.getPortablePath(child2.toString());

				if (Tools.equals(portable1, portable2)) {
					Assertions.assertEquals(LocalCommon.calculateId(portable1), LocalCommon.calculateId(portable2));
				} else {
					Assertions.assertNotEquals(LocalCommon.calculateId(portable1), LocalCommon.calculateId(portable2));
				}
			}
		}
	}

	@Test
	void testGetPortablePath() {
	}

}
