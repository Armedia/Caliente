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

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.function.CheckedSupplier;

public final class LocalCommon {
	public static final String TARGET_NAME = "local";
	public static final Set<String> TARGETS = Collections.singleton(LocalCommon.TARGET_NAME);

	private LocalCommon() {
	}

	public static <T> T uncheck(CheckedSupplier<T, IOException> s) {
		try {
			return s.getChecked();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	static Path getRootDirectory(CfgTools cfg) {
		String root = cfg.getString(LocalSetting.ROOT);
		if (root == null) { return null; }
		return Tools.canonicalize(Paths.get(root));
	}

	public static LocalRoot getLocalRoot(CfgTools cfg) throws IOException {
		Path root = LocalCommon.getRootDirectory(cfg);
		if (root == null) { throw new IllegalArgumentException("Must provide a root directory to work from"); }
		return new LocalRoot(root);
	}

	public static String calculateId(Path path) {
		if (path == null) { return null; }
		return LocalCommon.calculateId(path.toString());
	}

	public static String calculateId(String path) {
		path = LocalCommon.toPortablePath(path);
		if ((path == null) || Objects.equals("/", path)) { return null; }
		return DigestUtils.sha256Hex(path);
	}

	public static String toPortablePath(String path) {
		if (StringUtils.isEmpty(path)) { return null; }
		if (File.separatorChar != '/') {
			path = path.replace(File.separatorChar, '/');
		}
		return FileNameTools.reconstitute(FileNameTools.tokenize(path, '/'), true, false, '/');
	}

	public static String toLocalizedPath(String path) {
		return path.replace((File.separatorChar == '\\') ? '/' : '\\', File.separatorChar);
	}
}