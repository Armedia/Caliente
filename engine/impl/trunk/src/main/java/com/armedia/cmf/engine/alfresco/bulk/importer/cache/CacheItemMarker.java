package com.armedia.cmf.engine.alfresco.bulk.importer.cache;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

public class CacheItemMarker implements Cloneable {
	protected String name;

	protected boolean directory;

	protected Path localPath;

	protected String cmsPath;

	protected BigDecimal number;

	protected Path content;

	protected Path metadata;

	protected CacheItemMarker(CacheItemMarker copy) {
		if (copy == null) { throw new IllegalArgumentException("Must provide an object to base the copy off of"); }
		this.name = copy.name;
		this.directory = copy.directory;
		this.localPath = copy.localPath;
		this.cmsPath = copy.cmsPath;
		this.number = copy.number;
		this.content = copy.content;
		this.metadata = copy.metadata;
	}

	public CacheItemMarker() {
	}

	public boolean isDirectory() {
		return this.directory;
	}

	public void setDirectory(boolean directory) {
		this.directory = directory;
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
		return String.format("CacheItemMarker [name=%s, cmsPath=%s, number=%s, content=%s, metadata=%s]", this.name,
			this.cmsPath, this.number, this.content, this.metadata);
	}

	public CacheItemVersion getVersion() {
		CacheItemVersion version = new CacheItemVersion();
		version.setNumber(this.number);
		version.setContent(convertToSlashes(this.content.toString()));
		version.setMetadata(convertToSlashes(this.metadata.toString()));
		return version;
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