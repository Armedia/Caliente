package com.armedia.caliente.engine.alfresco.bulk.importer.cache;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {
	"directory", "name", "fsRelativePath", "relativePath", "versions"
})
@XmlRootElement(name = "item")
public class CacheItem {
	@XmlElement(name = "directory", required = true)
	protected boolean directory;

	@XmlElement(name = "name", required = true)
	protected String name;

	@XmlElement(name = "fsRelativePath", required = true)
	protected String fsRelativePath;

	@XmlElement(name = "relativePath", required = false)
	protected String relativePath;

	@XmlElementWrapper(name = "versions", required = true)
	@XmlElement(name = "version", required = true)
	protected List<CacheItemVersion> versions;

	public boolean isDirectory() {
		return this.directory;
	}

	public void setDirectory(boolean directory) {
		this.directory = directory;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFsRelativePath() {
		return this.fsRelativePath;
	}

	public void setFsRelativePath(String fsRelativePath) {
		this.fsRelativePath = fsRelativePath;
	}

	public String getRelativePath() {
		return this.relativePath;
	}

	public void setRelativePath(String relativePath) {
		this.relativePath = relativePath;
	}

	public List<CacheItemVersion> getVersions() {
		if (this.versions == null) {
			this.versions = new ArrayList<CacheItemVersion>();
		}
		return this.versions;
	}

	@Override
	public String toString() {
		return String.format("CacheItem [directory=%s, name=%s, fsRelativePath=%s, relativePath=%s, versions=%s]",
			this.directory, this.name, this.fsRelativePath, this.relativePath, this.versions);
	}
}