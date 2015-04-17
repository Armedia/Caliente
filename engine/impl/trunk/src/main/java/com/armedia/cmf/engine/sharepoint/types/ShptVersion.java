package com.armedia.cmf.engine.sharepoint.types;

import java.util.Date;

import com.armedia.cmf.engine.sharepoint.ShptVersionNumber;
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
		return this.version.isCurrentVersion();
	}

	public String getUrl() {
		return this.version.getUrl();
	}

	public String getLabel() {
		return this.version.getLabel();
	}
}
