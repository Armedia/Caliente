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
import java.nio.file.NoSuchFileException;
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

	private static final Logger LOG = LoggerFactory.getLogger(LocalRoot.class);
	private static final ConcurrentMap<FileStore, LocalCaseFolding> CASE_FOLDING = new ConcurrentHashMap<>();

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final Path path;
	private final LocalCaseFolding blindCaseFolding;

	private static LocalCaseFolding getCaseFolding(final Path testPath) throws IOException {
		try {
			return ConcurrentTools.createIfAbsent(LocalRoot.CASE_FOLDING, Files.getFileStore(testPath), (fs) -> {
				Path lower = LocalCaseFolding.LOWER.apply(testPath);
				Path upper = LocalCaseFolding.UPPER.apply(testPath);
				LocalCaseFolding caseFolding = (Files.isSameFile(upper, lower) ? LocalCaseFolding.UPPER
					: LocalCaseFolding.SAME);
				if (LocalRoot.LOG.isDebugEnabled()) {
					LocalRoot.LOG.debug("Computed case folding as {} for FileStore {} ({} @ {}) using path [{}]",
						caseFolding, fs, fs.type(), fs.hashCode(), testPath);
				}
				return caseFolding;
			});
		} catch (NoSuchFileException e) {
			// Maybe try to detect its parent's folding?
			Path parent = testPath.getParent();
			return (parent != null ? LocalRoot.getCaseFolding(parent) : LocalCaseFolding.SAME);
		}
	}

	private static Path foldCase(final Path testPath) throws IOException {
		return LocalRoot.getCaseFolding(testPath).apply(testPath);
	}

	public static String normalize(String path) throws IOException {
		String p = FilenameUtils.normalizeNoEndSeparator(path);
		if (p == null) { throw new IOException(String.format("The path [%s] contains too many '..' elements", path)); }
		return p;
	}

	public LocalRoot(String path) throws IOException {
		this(path, null);
	}

	public LocalRoot(String path, LocalCaseFolding blindCaseFolding) throws IOException {
		this(Paths.get(LocalRoot.normalize(path)), blindCaseFolding);
	}

	public LocalRoot(Path path) throws IOException {
		this(path, null);
	}

	public LocalRoot(Path path, LocalCaseFolding blindCaseFolding) throws IOException {
		this.path = LocalRoot
			.foldCase(Tools.canonicalize(Objects.requireNonNull(path, "Must provide a Path to use as the root")));
		this.blindCaseFolding = blindCaseFolding;
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

		// If we're avoiding accessing the disk for speed, then we simply do normalization
		// and case folding as needed
		if (this.blindCaseFolding != null) {
			String normalized = FilenameUtils.normalizeNoEndSeparator(newPath.toString());
			if (normalized == null) { return null; }
			return Paths.get(this.blindCaseFolding.fold(normalized));
		}

		Path canonical = Tools.canonicalize(newPath, false);
		if (canonical == null) { return null; }
		try {
			return LocalRoot.foldCase(canonical);
		} catch (IOException e) {
			throw new UncheckedIOException(
				String.format("Failed to test for filesystem case sensitivity with path [%s] (canonical = [%s])", path,
					canonical),
				e);
		}
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