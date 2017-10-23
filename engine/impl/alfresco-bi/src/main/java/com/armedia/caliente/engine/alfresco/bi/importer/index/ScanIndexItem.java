package com.armedia.caliente.engine.alfresco.bi.importer.index;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "item.t", propOrder = {
	"directory", "sourceName", "sourcePath", "targetName", "targetPath", "versions"
})
@XmlRootElement(name = "item")
public class ScanIndexItem {
	@XmlElement(name = "directory", required = true)
	protected boolean directory;

	@XmlElement(name = "targetName", required = true)
	protected String targetName;

	@XmlElement(name = "sourceName", required = true)
	protected String sourceName;

	@XmlElement(name = "targetPath", required = true)
	protected String targetPath;

	@XmlElement(name = "sourcePath", required = false)
	protected String sourcePath;

	@XmlElementWrapper(name = "versions", required = true)
	@XmlElement(name = "version", required = true)
	protected List<ScanIndexItemVersion> versions;

	public boolean isDirectory() {
		return this.directory;
	}

	public void setDirectory(boolean directory) {
		this.directory = directory;
	}

	public String getSourceName() {
		return this.sourceName;
	}

	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}

	public String getSourcePath() {
		return this.sourcePath;
	}

	public void setSourcePath(String sourcePath) {
		this.sourcePath = sourcePath;
	}

	public String getTargetName() {
		return this.targetName;
	}

	public void setTargetName(String targetName) {
		this.targetName = targetName;
	}

	public String getTargetPath() {
		return this.targetPath;
	}

	public void setTargetPath(String targetPath) {
		this.targetPath = targetPath;
	}

	public List<ScanIndexItemVersion> getVersions() {
		if (this.versions == null) {
			this.versions = new ArrayList<>();
		}
		return this.versions;
	}

	@Override
	public String toString() {
		return String.format(
			"ScanIndexItem [directory=%s, sourceName=%s, sourcePath=%s, targetName=%s, targetPath=%s, versions=%s]",
			this.directory, this.sourceName, this.sourcePath, this.targetName, this.targetPath, this.versions);
	}
}