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

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.armedia.caliente.engine.local.common.LocalCommon;
import com.armedia.caliente.engine.local.common.LocalRoot;
import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.LazyFormatter;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.function.LazySupplier;

class LocalFile {

	private static final String ENCODING = "UTF-8";

	public static String makeSafe(String s) throws IOException {
		return URLEncoder.encode(s, LocalFile.ENCODING);
	}

	public static String makeUnsafe(String s) throws IOException {
		return URLDecoder.decode(s, LocalFile.ENCODING);
	}

	public static String decodeSafePath(String safePath) throws IOException {
		List<String> r = new ArrayList<>();
		for (String s : FileNameTools.tokenize(safePath, '/')) {
			r.add(LocalFile.makeUnsafe(s));
		}
		return LocalRoot.normalize(FileNameTools.reconstitute(r, false, false));
	}

	public static LocalFile newFromSafePath(LocalRoot root, String safePath) throws IOException {
		return new LocalFile(root, LocalFile.decodeSafePath(safePath));
	}

	private final LocalRoot root;
	private final File absoluteFile;
	private final File relativeFile;
	private final String historyRadix;
	private final String versionTag;
	private final String safePath;
	private final String fullPath;
	private final String parentPath;
	private final String name;
	private final int pathCount;
	private final boolean folder;
	private final boolean regularFile;
	private final boolean symbolicLink;

	private final LazySupplier<String> id = new LazySupplier<>(() -> LocalCommon.calculateId(getPortableFullPath()));
	private final LazySupplier<String> parentId = new LazySupplier<>(
		() -> LocalCommon.calculateId(getPortableParentPath()));
	private final LazySupplier<String> historyId = new LazySupplier<>(
		() -> LocalCommon.calculateId(getPortableHistoryRadix()));

	private final LazySupplier<String> portableFullPath = new LazySupplier<>(
		() -> LocalCommon.getPortablePath(getFullPath()));
	private final LazySupplier<String> portableParentPath = new LazySupplier<>(() -> {
		String ppp = getParentPath();
		if (ppp == null) { return "/"; }
		return LocalCommon.getPortablePath(ppp);
	});
	private final LazySupplier<String> portableHistoryRadix = new LazySupplier<>(
		() -> LocalCommon.getPortablePath(getHistoryRadix()));

	private final LazySupplier<Integer> hash;
	private final LazyFormatter string;

	private LocalFile(LocalRoot root, String path) throws IOException {
		this.root = root;
		File f = root.relativize(new File(path));
		this.relativeFile = f;
		this.absoluteFile = root.makeAbsolute(f);

		List<String> r = new ArrayList<>();
		this.fullPath = this.relativeFile.getPath();
		for (String s : FileNameTools.tokenize(this.fullPath, File.separatorChar)) {
			r.add(LocalFile.makeSafe(s));
		}

		// The history radix is calculated from the relative portable filename, minus the version
		// data
		this.historyRadix = null;
		this.versionTag = null;

		this.safePath = FileNameTools.reconstitute(r, false, false, '/');
		this.pathCount = r.size();
		File parentFile = f.getParentFile();
		this.parentPath = (parentFile != null ? parentFile.getPath() : null);
		this.name = f.getName();

		Path p = this.absoluteFile.toPath();
		this.symbolicLink = Files.isSymbolicLink(p);
		this.regularFile = Files.isRegularFile(p);
		this.folder = Files.isDirectory(p);

		this.hash = new LazySupplier<>(() -> Tools.hashTool(this, null, this.root, this.absoluteFile));

		final String type = (this.folder ? "dir" : this.regularFile ? "file" : "<unknown>");
		this.string = LazyFormatter.of(
			"LocalFile [root=%s, absoluteFile=%s, relativeFile=%s, type=%s, link=%s, safePath=%s, fullPath=%s, parentPath=%s, name=%s, pathCount=%s]",
			this.root, this.absoluteFile, this.relativeFile, type, this.symbolicLink, this.safePath, this.fullPath,
			this.parentPath, this.name, this.pathCount);
	}

	public String getId() {
		return this.id.get();
	}

	public boolean isSameHistory(Path p) {
		// <> radix
		// [] constant, unimportant
		// {} version tag
		// () extra, constant, unimportant
		// <parent/basename>.[prefix.]{versionTag}[.suffix]
		// <parent/basename>/[prefix.]{versionTag}[.suffix]
		// <parent/basename>/[prefix.]{versionTag}[.suffix]/(stream)
		// The path may be absolute or relative. If it's relative, it's assumed to be relative
		// to the root of this instance.
		return false;
	}

	public String getPortableHistoryRadix() {
		return this.portableHistoryRadix.get();
	}

	public String getHistoryRadix() {
		return this.historyRadix;
	}

	public String getHistoryId() {
		return this.historyId.get();
	}

	public String getVersionTag() {
		return this.versionTag;
	}

	public String getParentId() {
		return this.parentId.get();
	}

	public boolean isFolder() {
		return this.folder;
	}

	public boolean isHeadRevision() {
		// TODO:
		return true;
	}

	public boolean isRegularFile() {
		return this.regularFile;
	}

	public boolean isSymbolicLink() {
		return this.symbolicLink;
	}

	public String getFullPath() {
		return this.fullPath;
	}

	public String getParentPath() {
		return this.parentPath;
	}

	public String getName() {
		return this.name;
	}

	public int getPathCount() {
		return this.pathCount;
	}

	public String getPortableParentPath() {
		return this.portableParentPath.get();
	}

	public String getPortableFullPath() {
		return this.portableFullPath.get();
	}

	/**
	 * <p>
	 * Returns a "universally-safe" path for the absoluteFile which escapes all
	 * potentially-dangerous characters with their URL-safe alternatives (i.e. ' ' -&gt; '+', %XX
	 * encoding, etc.). All path components are separated by forward slashes ('/'). Thus, the
	 * algorithm to obtain the original filename is to tokenize on forward slashes, URL-decode each
	 * component, and re-concatenate using the {@code File#separator} or {@code File#separatorChar}.
	 * </p>
	 *
	 * @return str
	 */
	public String getSafePath() {
		return this.safePath;
	}

	public File getRelative() {
		return this.relativeFile;
	}

	public File getAbsolute() {
		return this.absoluteFile;
	}

	public LocalRoot getRootPath() {
		return this.root;
	}

	@Override
	public int hashCode() {
		return this.hash.get();
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		LocalFile other = LocalFile.class.cast(obj);
		if (!Objects.equals(this.root, other.root)) { return false; }
		if (!Objects.equals(this.absoluteFile, other.absoluteFile)) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return this.string.get();
	}

	public static LocalFile getInstance(LocalRoot root, String path) throws IOException {

		// Here we have to do the following analysis:
		//
		// 1) Is this a folder? If so, just return the new instance
		// 2) Find the file's entire history
		// 3) Stow the file's entire history "somewhere" (an LRU cache?)
		// 4) Find the specific file within its history
		// 5) Return the instance
		//
		// The returned instance should be able to link back to its cached history
		// or be used to retrieve it or rediscover it

		return new LocalFile(root, path);
	}
}