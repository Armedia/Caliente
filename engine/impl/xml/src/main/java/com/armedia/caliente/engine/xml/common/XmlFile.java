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
package com.armedia.caliente.engine.xml.common;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.Tools;

public class XmlFile {

	private static final String ENCODING = "UTF-8";

	private static String makeSafe(String s) throws IOException {
		return URLEncoder.encode(s, XmlFile.ENCODING);
	}

	private static String makeUnsafe(String s) throws IOException {
		return URLDecoder.decode(s, XmlFile.ENCODING);
	}

	public static String decodeSafePath(String safePath) throws IOException {
		List<String> r = new ArrayList<>();
		for (String s : FileNameTools.tokenize(safePath, '/')) {
			r.add(XmlFile.makeUnsafe(s));
		}
		return XmlRoot.normalize(FileNameTools.reconstitute(r, false, false));
	}

	public static XmlFile newFromSafePath(XmlRoot root, String safePath) throws IOException {
		return new XmlFile(root, XmlFile.decodeSafePath(safePath));
	}

	private final XmlRoot root;
	private final Path absolutePath;
	private final Path relativePath;
	private final String safePath;
	private final String path;
	private final String name;
	private final int pathCount;

	public XmlFile(XmlRoot root, String path) throws IOException {
		this.root = root;
		Path p = Paths.get(root.relativize(path));
		this.relativePath = p;
		this.absolutePath = root.makeAbsolute(p);

		List<String> r = new ArrayList<>();
		for (String s : FileNameTools.tokenize(this.relativePath.toString())) {
			r.add(XmlFile.makeSafe(s));
		}
		this.safePath = FileNameTools.reconstitute(r, false, false, '/');
		this.pathCount = r.size();
		this.path = p.getParent().toString();
		this.name = p.getFileName().toString();

	}

	public String getPathHash() {
		return String.format("%08x", this.relativePath.hashCode());
	}

	public String getPath() {
		return this.path;
	}

	public String getName() {
		return this.name;
	}

	public int getPathCount() {
		return this.pathCount;
	}

	public String getPortablePath() {
		String path = getPath();
		if (path == null) { return null; }
		return FileNameTools.reconstitute(FileNameTools.tokenize(path), true, false, '/');
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

	public Path getRelative() {
		return this.relativePath;
	}

	public Path getAbsolute() {
		return this.absolutePath;
	}

	public XmlRoot getRootPath() {
		return this.root;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.root, this.absolutePath);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		XmlFile other = XmlFile.class.cast(obj);
		if (!Objects.equals(this.root, other.root)) { return false; }
		if (!Objects.equals(this.absolutePath, other.absolutePath)) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format(
			"XmlFile [root=%s, absoluteFile=%s, relativeFile=%s, safePath=%s, path=%s, name=%s, pathCount=%s]",
			this.root, this.absolutePath, this.relativePath, this.safePath, this.path, this.name, this.pathCount);
	}
}
