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
package com.armedia.caliente.tools.alfresco.bi.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.armedia.commons.utilities.Tools;

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
	public int hashCode() {
		return Tools.hashTool(this, null, this.directory, this.sourceName, this.sourcePath, this.targetName,
			this.targetPath, this.versions.hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		ScanIndexItem other = ScanIndexItem.class.cast(obj);
		if (this.directory != other.directory) { return false; }
		if (!Tools.equals(this.sourceName, other.sourceName)) { return false; }
		if (!Tools.equals(this.sourcePath, other.sourcePath)) { return false; }
		if (!Tools.equals(this.targetName, other.targetName)) { return false; }
		if (!Tools.equals(this.targetPath, other.targetPath)) { return false; }
		if (!Tools.equals(this.versions.size(), other.versions.size())) { return false; }
		if (!Tools.equals(this.versions, other.versions)) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format(
			"ScanIndexItem [directory=%s, sourceName=%s, sourcePath=%s, targetName=%s, targetPath=%s, versions=%s]",
			this.directory, this.sourceName, this.sourcePath, this.targetName, this.targetPath, this.versions);
	}
}