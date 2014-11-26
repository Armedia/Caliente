package com.armedia.cmf.engine.sharepoint;

import java.util.Date;

import com.armedia.cmf.storage.StoredObjectType;
import com.independentsoft.share.CheckOutType;
import com.independentsoft.share.CustomizedPageStatus;
import com.independentsoft.share.File;
import com.independentsoft.share.FileLevel;

public class ShptFile extends ShptObject<File> {

	public ShptFile(File file) {
		super(file, StoredObjectType.DOCUMENT);
	}

	@Override
	public String getId() {
		return this.wrapped.getUniqueId();
	}

	@Override
	public String getName() {
		return this.wrapped.getName();
	}

	@Override
	public String getServerRelativeUrl() {
		return this.wrapped.getServerRelativeUrl();
	}

	@Override
	public Date getCreatedTime() {
		return this.wrapped.getCreatedTime();
	}

	@Override
	public Date getLastModifiedTime() {
		return this.wrapped.getLastModifiedTime();
	}

	public String getCheckInComment() {
		return this.wrapped.getCheckInComment();
	}

	public CheckOutType getCheckOutType() {
		return this.wrapped.getCheckOutType();
	}

	public String getContentTag() {
		return this.wrapped.getContentTag();
	}

	public CustomizedPageStatus getCustomizedPageStatus() {
		return this.wrapped.getCustomizedPageStatus();
	}

	public String getETag() {
		return this.wrapped.getETag();
	}

	public boolean exists() {
		return this.wrapped.exists();
	}

	public long getLength() {
		return this.wrapped.getLength();
	}

	public String getLinkingUrl() {
		return this.wrapped.getLinkingUrl();
	}

	public FileLevel getLevel() {
		return this.wrapped.getLevel();
	}

	public int getMajorVersion() {
		return this.wrapped.getMajorVersion();
	}

	public int getMinorVersion() {
		return this.wrapped.getMinorVersion();
	}

	public String getTitle() {
		return this.wrapped.getTitle();
	}

	public int getUIVersion() {
		return this.wrapped.getUIVersion();
	}

	public String getUIVersionLabel() {
		return this.wrapped.getUIVersionLabel();
	}
}