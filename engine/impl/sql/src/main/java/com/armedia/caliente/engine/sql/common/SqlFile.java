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
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.Tools;

public class SqlFile {

	private static final String ENCODING = "UTF-8";

	private static String makeSafe(String s) throws IOException {
		return URLEncoder.encode(s, SqlFile.ENCODING);
	}

	private static String makeUnsafe(String s) throws IOException {
		return URLDecoder.decode(s, SqlFile.ENCODING);
	}

	public static String decodeSafePath(String safePath) throws IOException {
		List<String> r = new ArrayList<>();
		for (String s : FileNameTools.tokenize(safePath, '/')) {
			r.add(SqlFile.makeUnsafe(s));
		}
		return SqlRoot.normalize(FileNameTools.reconstitute(r, false, false));
	}

	public static SqlFile newFromSafePath(SqlRoot root, String safePath) throws IOException {
		return new SqlFile(root, SqlFile.decodeSafePath(safePath));
	}

	private final SqlRoot root;
	private final File absoluteFile;
	private final File relativeFile;
	private final String safePath;
	private final String fullPath;
	private final String parentPath;
	private final String name;
	private final int pathCount;
	private final boolean folder;
	private final boolean regularFile;
	private final boolean symbolicLink;

	public SqlFile(SqlRoot root, String path) throws IOException {
		this.root = root;
		File f = root.relativize(new File(path));
		this.relativeFile = f;
		this.absoluteFile = root.makeAbsolute(f);

		List<String> r = new ArrayList<>();
		this.fullPath = this.relativeFile.getPath();
		for (String s : FileNameTools.tokenize(this.fullPath)) {
			r.add(SqlFile.makeSafe(s));
		}
		this.safePath = FileNameTools.reconstitute(r, false, false, '/');
		this.pathCount = r.size();
		File parentFile = f.getParentFile();
		this.parentPath = (parentFile != null ? parentFile.getPath() : null);
		this.name = f.getName();

		Path p = this.absoluteFile.toPath();
		this.symbolicLink = Files.isSymbolicLink(p);
		this.regularFile = Files.isRegularFile(p);
		this.folder = Files.isDirectory(p);
	}

	public String getId() {
		return SqlCommon.calculateId(getPortableFullPath());
	}

	public String getParentId() {
		return SqlCommon.calculateId(getPortableParentPath());
	}

	public boolean isFolder() {
		return this.folder;
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
		String path = getParentPath();
		if (path == null) { return "/"; }
		return SqlCommon.getPortablePath(path);
	}

	public String getPortableFullPath() {
		return SqlCommon.getPortablePath(getFullPath());
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

	public SqlRoot getRootPath() {
		return this.root;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.root, this.absoluteFile);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		SqlFile other = SqlFile.class.cast(obj);
		if (!Objects.equals(this.root, other.root)) { return false; }
		if (!Objects.equals(this.absoluteFile, other.absoluteFile)) { return false; }
		return true;
	}

	@Override
	public String toString() {
		String type = (this.folder ? "dir" : this.regularFile ? "file" : "<unknown>");
		return String.format(
			"SqlFile [root=%s, absoluteFile=%s, relativeFile=%s, type=%s, link=%s, safePath=%s, fullPath=%s, parentPath=%s, name=%s, pathCount=%s]",
			this.root, this.absoluteFile, this.relativeFile, type, this.symbolicLink, this.safePath, this.fullPath,
			this.parentPath, this.name, this.pathCount);
	}
}
