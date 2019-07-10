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
package com.armedia.caliente.engine.sharepoint.exporter;

import java.util.Date;

import com.armedia.caliente.engine.sharepoint.ShptVersionNumber;
import com.independentsoft.share.CheckOutType;
import com.independentsoft.share.CustomizedPageStatus;
import com.independentsoft.share.File;
import com.independentsoft.share.FileLevel;
import com.independentsoft.share.FileVersion;

public final class ShptVersion {

	private final File file;

	private final FileVersion version;

	private final ShptVersionNumber versionNumber;

	public ShptVersion(File file) {
		this(file, null);
	}

	public ShptVersion(File file, FileVersion version) {
		this.file = file;
		this.version = version;
		if (version == null) {
			this.versionNumber = new ShptVersionNumber(file.getMajorVersion(), file.getMinorVersion());
		} else {
			this.versionNumber = new ShptVersionNumber(version.getLabel());
		}
	}

	public File getFile() {
		return this.file;
	}

	public FileVersion getVersion() {
		return this.version;
	}

	public ShptVersionNumber getVersionNumber() {
		return this.versionNumber;
	}

	public String getCheckInComment() {
		return this.file.getCheckInComment();
	}

	public CheckOutType getCheckOutType() {
		return this.file.getCheckOutType();
	}

	public String getContentTag() {
		return this.file.getContentTag();
	}

	public CustomizedPageStatus getCustomizedPageStatus() {
		return this.file.getCustomizedPageStatus();
	}

	public String getETag() {
		return this.file.getETag();
	}

	public boolean exists() {
		return this.file.exists();
	}

	public long getLength() {
		return this.file.getLength();
	}

	public String getLinkingUrl() {
		return this.file.getLinkingUrl();
	}

	public FileLevel getLevel() {
		return this.file.getLevel();
	}

	public int getMajorVersion() {
		return this.file.getMajorVersion();
	}

	public int getMinorVersion() {
		return this.file.getMinorVersion();
	}

	public String getName() {
		return this.file.getName();
	}

	public String getServerRelativeUrl() {
		return this.file.getServerRelativeUrl();
	}

	public Date getCreatedTime() {
		return this.file.getCreatedTime();
	}

	public Date getLastModifiedTime() {
		return this.file.getLastModifiedTime();
	}

	public String getTitle() {
		return this.file.getTitle();
	}

	public int getUIVersion() {
		return this.file.getUIVersion();
	}

	public String getUIVersionLabel() {
		return this.file.getUIVersionLabel();
	}

	public String getUniqueId() {
		return this.file.getUniqueId();
	}

	public String getId() {
		return this.version.getId();
	}

	public boolean isCurrentVersion() {
		return (this.version == null) || this.version.isCurrentVersion();
	}

	public String getUrl() {
		return this.version.getUrl();
	}

	public String getLabel() {
		return this.version.getLabel();
	}
}
