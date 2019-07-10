/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
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
package com.armedia.caliente.engine.alfresco.bi.importer;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import com.armedia.caliente.tools.alfresco.bi.xml.ScanIndexItem;
import com.armedia.caliente.tools.alfresco.bi.xml.ScanIndexItemVersion;

public class ScanIndexItemMarker implements Cloneable {
	public static enum MarkerType {
		//
		NORMAL, // A standalone file or folder
		RENDITION_ROOT(true), // The root directory that contains all renditions
		RENDITION_TYPE(true), // The directory that contains each rendition type
		RENDITION_ENTRY, // The renditions themselves
		VDOC_ROOT(true), // A Virtual Document's root directory
		VDOC_VERSION(true), // A Virtual Document version's directory
		VDOC_STREAM, // A Virtual Document's primary stream
		VDOC_RENDITION, // A Virtual Document's rendition
		VDOC_REFERENCE, // A Virtual Document reference to another document
		//
		;

		private final Boolean staticValue;

		private MarkerType() {
			this(null);
		}

		private MarkerType(Boolean value) {
			this.staticValue = value;
		}

		public boolean isFolder(File contentFile) {
			if (this.staticValue != null) { return this.staticValue.booleanValue(); }
			return !contentFile.exists() || contentFile.isDirectory();
		}
	}

	private final MarkerType type;

	private boolean directory;

	private int index;

	private int headIndex;

	private int versionCount;

	private Path sourcePath;

	private String sourceName;

	private String targetPath;

	private String targetName;

	private BigDecimal number;

	private Path content;

	private Path metadata;

	protected ScanIndexItemMarker(ScanIndexItemMarker copy) {
		if (copy == null) { throw new IllegalArgumentException("Must provide an object to base the copy off of"); }
		this.type = copy.type;
		this.directory = copy.directory;
		this.sourcePath = copy.sourcePath;
		this.sourceName = copy.sourceName;
		this.targetPath = copy.targetPath;
		this.targetName = copy.targetName;
		this.number = copy.number;
		this.content = copy.content;
		this.metadata = copy.metadata;
	}

	public ScanIndexItemMarker(MarkerType type) {
		this.type = type;
	}

	public ScanIndexItemMarker() {
		this(MarkerType.NORMAL);
	}

	public MarkerType getType() {
		return this.type;
	}

	public boolean isDirectory() {
		return this.directory;
	}

	public void setDirectory(boolean directory) {
		this.directory = directory;
	}

	public int getThisIndex() {
		return this.index;
	}

	public void setIndex(int thisIndex) {
		this.index = thisIndex;
	}

	public int getHeadIndex() {
		return this.headIndex;
	}

	public void setHeadIndex(int headIndex) {
		this.headIndex = headIndex;
	}

	public int getVersionCount() {
		return this.versionCount;
	}

	public void setVersionCount(int versionCount) {
		this.versionCount = versionCount;
	}

	public boolean isFirstVersion() {
		return (this.index == 1);
	}

	public boolean isLastVersion() {
		return (this.index == this.versionCount);
	}

	public boolean isHeadVersion() {
		return (this.index == this.headIndex);
	}

	public Path getSourcePath() {
		return this.sourcePath;
	}

	public void setSourcePath(Path sourcePath) {
		this.sourcePath = sourcePath;
	}

	public String getSourceName() {
		return this.sourceName;
	}

	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}

	public String getTargetPath() {
		return this.targetPath;
	}

	public void setTargetPath(String targetPath) {
		this.targetPath = targetPath;
	}

	public String getTargetName() {
		return this.targetName;
	}

	public void setTargetName(String targetName) {
		this.targetName = targetName;
	}

	public BigDecimal getNumber() {
		return this.number;
	}

	public void setNumber(BigDecimal number) {
		this.number = number;
	}

	public Path getContent() {
		return this.content;
	}

	public void setContent(Path content) {
		this.content = content;
	}

	public Path getMetadata() {
		return this.metadata;
	}

	public void setMetadata(Path metadata) {
		this.metadata = metadata;
	}

	@Override
	public String toString() {
		return String.format(
			"ScanIndexItemMarker [type=%s, directory=%s, thisIndex=%s, headIndex=%s, versionCount=%s, sourcePath=%s, sourceName=%s, targetPath=%s, targetName=%s, number=%s, content=%s, metadata=%s]",
			this.type, this.directory, this.index, this.headIndex, this.versionCount, this.sourcePath, this.sourceName,
			this.targetPath, this.targetName, this.number, this.content, this.metadata);
	}

	public ScanIndexItemVersion getVersion() {
		ScanIndexItemVersion version = new ScanIndexItemVersion();
		version.setNumber(this.number);
		if (this.content != null) {
			version.setContent(convertToSlashes(this.content.toString()));
		} else {
			version.setContent(null);
		}
		if (this.metadata != null) {
			version.setMetadata(convertToSlashes(this.metadata.toString()));
		} else {
			version.setMetadata(null);
		}
		return version;
	}

	public ScanIndexItem getItem() {
		return getItem(Collections.singletonList(this));
	}

	public ScanIndexItem getItem(List<ScanIndexItemMarker> markers) {
		ScanIndexItem item = new ScanIndexItem();
		item.setDirectory(this.directory);
		if (this.sourcePath != null) {
			item.setSourcePath(convertToSlashes(this.sourcePath.toString()));
		}
		if (this.sourceName != null) {
			item.setSourceName(this.sourceName);
		}
		item.setTargetPath(this.targetPath);
		item.setTargetName(this.targetName);
		List<ScanIndexItemVersion> versions = item.getVersions();
		for (ScanIndexItemMarker m : markers) {
			versions.add(m.getVersion());
		}
		return item;
	}

	private final String convertToSlashes(String path) {
		if (File.separatorChar == '/') { return path; }
		// We only intend to store paths separated by '/' in the XML, regardless of source
		// platform. This is CRITICAL. We make the effort to change all the separators
		// into '/'
		return path.replaceAll(String.format("\\Q%s\\E+", File.separator), "/");
	}

	@Override
	public ScanIndexItemMarker clone() {
		return new ScanIndexItemMarker(this);
	}
}