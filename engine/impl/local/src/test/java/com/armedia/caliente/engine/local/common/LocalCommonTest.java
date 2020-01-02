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
package com.armedia.caliente.engine.local.common;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

		String s = null;
		Path path = null;

		s = "";
		settings.put(LocalSetting.ROOT.getLabel(), s);
		path = LocalCommon.getRootDirectory(cfg);
		Assertions.assertNull(path);

		s = "/some/folder/path";
		settings.put(LocalSetting.ROOT.getLabel(), s);
		path = LocalCommon.getRootDirectory(cfg);
		Assertions.assertEquals(s, path.toString());

		s = "a/relative/variation/some/folder/path/../another/././../target";
		settings.put(LocalSetting.ROOT.getLabel(), s);
		path = Tools.canonicalize(Paths.get(s));
		s = path.toString();
		path = LocalCommon.getRootDirectory(cfg);
		Assertions.assertEquals(s, path.toString());

		s = "/some/folder/path/../another/././../target";
		settings.put(LocalSetting.ROOT.getLabel(), s);
		path = Tools.canonicalize(Paths.get(s));
		s = path.toString();
		path = LocalCommon.getRootDirectory(cfg);
		Assertions.assertEquals(s, path.toString());
	}

	@Test
	void testCalculateId() {
		String[] paths = {
			"a", "b", //
			"c/d/../e", "f/g/h/i/j/k/../l", "a/././././b", //
		};
		final Path root = Tools.canonicalize(FileUtils.getUserDirectory()).toPath();
		for (String path1 : paths) {
			path1 = FilenameUtils.normalize(path1);
			Assertions.assertNotNull(path1,
				String.format("The path [%s] is a bad test example - fix the unit test", path1));
			Path child1 = root.resolve(path1).normalize();
			child1 = root.relativize(child1);

			for (String path2 : paths) {
				path2 = FilenameUtils.normalize(path2);
				Assertions.assertNotNull(path2,
					String.format("The path [%s] is a bad test example - fix the unit test", path2));
				Path child2 = root.resolve(path2).normalize();
				child2 = root.relativize(child2);

				if (Objects.equals(child1.toString(), child2.toString())) {
					Assertions.assertEquals(LocalCommon.calculateId(child1.toString()),
						LocalCommon.calculateId(child2.toString()));
				} else {
					Assertions.assertNotEquals(LocalCommon.calculateId(child1.toString()),
						LocalCommon.calculateId(child2.toString()));
				}
			}
		}
	}

	@Test
	void testGetPortablePath() {
	}

}
