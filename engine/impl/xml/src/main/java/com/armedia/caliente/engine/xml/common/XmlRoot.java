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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import org.apache.commons.io.FilenameUtils;

import com.armedia.commons.utilities.Tools;

public final class XmlRoot implements Comparable<XmlRoot> {

	private final Path path;
	private final Path metadataRoot;

	static String normalize(String path) {
		String p2 = FilenameUtils.normalize(path);
		if (p2 == null) {
			throw new RuntimeException(String.format("The path [%s] contains too many '..' elements", path));
		}
		return p2;
	}

	public XmlRoot(Path path) throws IOException {
		this.path = path.toAbsolutePath();
		this.metadataRoot = this.path.resolve(XmlCommon.METADATA_ROOT);
	}

	public Path getPath() {
		return this.path;
	}

	public Path getMetadataRoot() {
		return this.metadataRoot;
	}

	public String relativize(String path) throws IOException {
		Path p = relativize(Paths.get(XmlRoot.normalize(path)));
		String str = p.toString();
		if (str.startsWith(File.separator)) { return str.substring(1); }
		throw new IOException(String.format("The path [%s] is not a child of [%s]", path, this.metadataRoot));
	}

	public Path relativize(Path p) {
		p = p.normalize();
		if (!p.isAbsolute()) {
			p = this.metadataRoot.resolve(p);
		}
		return this.metadataRoot.relativize(p);
	}

	public Path makeAbsolute(Path path) {
		if (path.isAbsolute()) { return path; }
		return this.metadataRoot.resolve(path);
	}

	public Path makeAbsolute(String path) {
		return makeAbsolute(XmlRoot.normalize(path));
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.path);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		XmlRoot other = XmlRoot.class.cast(obj);
		if (!Objects.equals(this.path, other.path)) { return false; }
		return true;
	}

	@Override
	public int compareTo(XmlRoot o) {
		if (o == null) { return 1; }
		return this.path.compareTo(o.path);
	}

	@Override
	public String toString() {
		return String.format("XmlRoot [%s]", this.path);
	}
}