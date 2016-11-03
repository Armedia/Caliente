package com.armedia.cmf.engine.alfresco.bulk.importer.cache;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class CacheItemMarker implements Cloneable {
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
			return contentFile.isDirectory();
		}
	}

	private final MarkerType type;

	private String name;

	private boolean directory;

	private int index;

	private int headIndex;

	private int versionCount;

	private Path localPath;

	private String cmsPath;

	private BigDecimal number;

	private Path content;

	private Path metadata;

	protected CacheItemMarker(CacheItemMarker copy) {
		if (copy == null) { throw new IllegalArgumentException("Must provide an object to base the copy off of"); }
		this.type = copy.type;
		this.name = copy.name;
		this.directory = copy.directory;
		this.localPath = copy.localPath;
		this.cmsPath = copy.cmsPath;
		this.number = copy.number;
		this.content = copy.content;
		this.metadata = copy.metadata;
	}

	public CacheItemMarker(MarkerType type) {
		this.type = type;
	}

	public CacheItemMarker() {
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

	public Path getLocalPath() {
		return this.localPath;
	}

	public void setLocalPath(Path localPath) {
		this.localPath = localPath;
	}

	public String getCmsPath() {
		return this.cmsPath;
	}

	public void setCmsPath(String cmsPath) {
		this.cmsPath = cmsPath;
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

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return String.format(
			"CacheItemMarker [type=%s, name=%s, directory=%s, thisIndex=%s, headIndex=%s, versionCount=%s, localPath=%s, cmsPath=%s, number=%s, content=%s, metadata=%s]",
			this.type, this.name, this.directory, this.index, this.headIndex, this.versionCount, this.localPath,
			this.cmsPath, this.number, this.content, this.metadata);
	}

	public CacheItemVersion getVersion() {
		CacheItemVersion version = new CacheItemVersion();
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

	public CacheItem getItem() {
		return getItem(Collections.singletonList(this));
	}

	public CacheItem getItem(List<CacheItemMarker> markers) {
		CacheItem item = new CacheItem();
		item.setName(this.name);
		item.setDirectory(this.directory);
		item.setFsRelativePath(convertToSlashes(this.localPath.toString()));
		item.setRelativePath(this.cmsPath);
		List<CacheItemVersion> versions = item.getVersions();
		for (CacheItemMarker m : markers) {
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
	public CacheItemMarker clone() {
		return new CacheItemMarker(this);
	}
}