/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
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
package com.armedia.caliente.engine.sql.common;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

class SqlCommonTest {

	@Test
	void testGetRootDirectory() {
		Assertions.assertThrows(NullPointerException.class, () -> SqlCommon.getRootDirectory(null));
		Map<String, String> settings = new HashMap<>();
		CfgTools cfg = new CfgTools(settings);
		Assertions.assertNull(SqlCommon.getRootDirectory(cfg));

		String path = null;
		File file = null;

		path = "";
		settings.put(SqlSetting.ROOT.getLabel(), path);
		file = SqlCommon.getRootDirectory(cfg);
		Assertions.assertNull(file);

		path = "/some/folder/path";
		settings.put(SqlSetting.ROOT.getLabel(), path);
		file = SqlCommon.getRootDirectory(cfg);
		Assertions.assertEquals(path, file.getPath());

		path = "a/relative/variation/some/folder/path/../another/././../target";
		settings.put(SqlSetting.ROOT.getLabel(), path);
		file = Tools.canonicalize(new File(path));
		path = file.getPath();
		file = SqlCommon.getRootDirectory(cfg);
		Assertions.assertEquals(path, file.getPath());

		path = "/some/folder/path/../another/././../target";
		settings.put(SqlSetting.ROOT.getLabel(), path);
		file = Tools.canonicalize(new File(path));
		path = file.getPath();
		file = SqlCommon.getRootDirectory(cfg);
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
			String portable1 = SqlCommon.getPortablePath(child1.toString());

			for (String path2 : paths) {
				path2 = FilenameUtils.normalize(path2);
				Assertions.assertNotNull(path2,
					String.format("The path [%s] is a bad test example - fix the unit test", path2));
				Path child2 = root.resolve(path2).normalize();
				child2 = root.relativize(child2);
				String portable2 = SqlCommon.getPortablePath(child2.toString());

				if (Objects.equals(portable1, portable2)) {
					Assertions.assertEquals(SqlCommon.calculateId(portable1), SqlCommon.calculateId(portable2));
				} else {
					Assertions.assertNotEquals(SqlCommon.calculateId(portable1), SqlCommon.calculateId(portable2));
				}
			}
		}
	}

	@Test
	void testGetPortablePath() {
	}

}
