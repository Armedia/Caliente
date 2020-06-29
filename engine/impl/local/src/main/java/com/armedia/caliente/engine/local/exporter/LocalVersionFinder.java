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
package com.armedia.caliente.engine.local.exporter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.local.common.LocalRoot;

public interface LocalVersionFinder {

	public static final Function<Path, Path> PATH_IDENTITY = Function.identity();

	public static Path convert(Path path, Function<String, String> converter) {
		if ((path == null) || (converter == null)) { return path; }
		Path newPath = null;
		if (path.isAbsolute()) {
			Path parent = path.getParent();
			if (parent != null) {
				newPath = parent;
			}
		}
		for (Path p : path) {
			String P = converter.apply(p.toString());
			if (newPath == null) {
				newPath = Paths.get(P);
			} else {
				newPath = newPath.resolve(P);
			}
		}
		return newPath;
	}

	public static Path upperCase(Path path) {
		return LocalVersionFinder.convert(path, StringUtils::upperCase);
	}

	public static Path lowerCase(Path path) {
		return LocalVersionFinder.convert(path, StringUtils::lowerCase);
	}

	public default String getHistoryId(final LocalRoot root, final Path path) throws Exception {
		return getHistoryId(root, path, LocalVersionFinder.PATH_IDENTITY);
	}

	public String getHistoryId(final LocalRoot root, final Path path, final Function<Path, Path> pathConverter)
		throws Exception;

	public default LocalVersionHistory getFullHistory(final LocalRoot root, final Path path) throws Exception {
		return getFullHistory(root, path, LocalVersionFinder.PATH_IDENTITY);
	}

	public LocalVersionHistory getFullHistory(final LocalRoot root, final Path path,
		final Function<Path, Path> pathConverter) throws Exception;

}