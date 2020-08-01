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
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.concurrent.ConcurrentTools;

public final class LocalRoot implements Comparable<LocalRoot> {

	protected static final Path ROOT = Paths.get(File.separator);
	protected static final LocalCaseFolding DEFAULT_CASE_FOLDING = LocalCaseFolding.NONE;

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final Path path;
	private final ConcurrentMap<FileStore, LocalCaseFolding> caseFolding = new ConcurrentHashMap<>();

	public static String normalize(String path) throws IOException {
		String p = FilenameUtils.normalize(path);
		if (p == null) { throw new IOException(String.format("The path [%s] contains too many '..' elements", path)); }
		return p;
	}

	public LocalRoot(String path) throws IOException {
		this(new File(LocalRoot.normalize(path)).toPath());
	}

	public LocalRoot(Path path) throws IOException {
		this.path = Tools.canonicalize(Objects.requireNonNull(path, "Must provide a Path to use as the root"));
	}

	private Path foldCase(final Path testPath) {
		try {
			return ConcurrentTools.createIfAbsent(this.caseFolding, Files.getFileStore(testPath), (fs) -> {
				Path lower = LocalCaseFolding.LOWER.apply(testPath);
				Path upper = LocalCaseFolding.UPPER.apply(testPath);
				LocalCaseFolding caseFolding = (Files.isSameFile(upper, lower) ? LocalCaseFolding.UPPER
					: LocalCaseFolding.NONE);
				if (this.log.isDebugEnabled()) {
					this.log.debug("Computed case folding as {} for FileStore {} ({} @ {}) using path [{}]",
						caseFolding, fs, fs.type(), fs.hashCode(), testPath);
				}
				return caseFolding;
			}).apply(testPath);
		} catch (IOException e) {
			throw new UncheckedIOException(
				String.format("Failed to test for filesystem case sensitivity with path [%s]", testPath), e);
		}
	}

	public Path getPath() {
		return this.path;
	}

	public String relativize(String path) throws IOException {
		if (StringUtils.isEmpty(path)) { return File.separator; }
		path = LocalRoot.normalize(path);
		Path p = Paths.get(path);
		p = relativize(p);
		return Tools.toString(p);
	}

	public Path relativize(Path path) throws IOException {
		if ((path == null) || (path.getNameCount() < 1)) { return LocalRoot.ROOT; }
		if (!path.isAbsolute()) {
			path = this.path.resolve(path);
		}
		path = path.normalize();
		if (!path.startsWith(this.path)) {
			throw new IOException(String.format("The path [%s] is not a child of [%s]", path, this.path));
		}
		return this.path.relativize(path);
	}

	public Path makeAbsolute(String path) {
		return makeAbsolute(Paths.get(path));
	}

	public Path makeAbsolute(Path path) {
		Path newPath = this.path.resolve(path);
		Path canonical = Tools.canonicalize(newPath, false);
		return foldCase(canonical);
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.path);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		LocalRoot other = LocalRoot.class.cast(obj);
		if (!Objects.equals(this.path, other.path)) { return false; }
		return true;
	}

	@Override
	public int compareTo(LocalRoot o) {
		if (o == null) { return 1; }
		return this.path.compareTo(o.path);
	}

	@Override
	public String toString() {
		return String.format("LocalRoot [%s]", this.path);
	}
}