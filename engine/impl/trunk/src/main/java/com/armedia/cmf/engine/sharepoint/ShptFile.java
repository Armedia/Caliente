package com.armedia.cmf.engine.sharepoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.sharepoint.exporter.ShptExportContext;
import com.armedia.cmf.storage.StoredAttribute;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredProperty;
import com.armedia.cmf.storage.StoredValue;
import com.independentsoft.share.CheckOutType;
import com.independentsoft.share.CustomizedPageStatus;
import com.independentsoft.share.File;
import com.independentsoft.share.FileLevel;
import com.independentsoft.share.FileVersion;
import com.independentsoft.share.Service;
import com.independentsoft.share.ServiceException;

public class ShptFile extends ShptFSObject<File> {

	public ShptFile(Service service, File file) {
		super(service, file, StoredObjectType.DOCUMENT);
	}

	@Override
	public String getSearchKey() {
		return this.wrapped.getServerRelativeUrl();
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

	@Override
	public String getBatchId() {
		return this.wrapped.getUniqueId();
	}

	@Override
	public String getLabel() {
		return this.wrapped.getName();
	}

	@Override
	protected void marshal(StoredObject<StoredValue> object) throws ExportException {
		super.marshal(object);
		List<StoredValue> versionNames = new ArrayList<StoredValue>();
		versionNames.add(new StoredValue(String.format("%d.%d", this.wrapped.getMajorVersion(),
			this.wrapped.getMinorVersion())));
		versionNames.add(new StoredValue("CURRENT")); // temporary hardcode
		try {
			List<FileVersion> l = this.service.getFileVersions(this.wrapped.getServerRelativeUrl());
			for (FileVersion v : l) {
				// TODO: determine if it's the current version, or this version, etc...
				v.hashCode();
			}
		} catch (ServiceException e) {

		}
		object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.VERSION.name, StoredDataType.STRING, true,
			versionNames));
	}

	@Override
	protected Collection<ShptObject<?>> findRequirements(Service service, StoredObject<StoredValue> marshaled,
		ShptExportContext ctx) throws Exception {
		Collection<ShptObject<?>> ret = super.findRequirements(service, marshaled, ctx);
		ret.add(new ShptUser(service, service.getFileAuthor(this.wrapped.getServerRelativeUrl())));
		ShptContent content = new ShptContent(service, this.wrapped);
		ret.add(content);
		marshaled.setProperty(new StoredProperty<StoredValue>(ShptProperties.CONTENTS.name, StoredDataType.ID, false,
			new StoredValue(StoredDataType.ID, content.getId())));
		return ret;
	}

	@Override
	protected Collection<ShptObject<?>> findDependents(Service service, StoredObject<StoredValue> marshaled,
		ShptExportContext ctx) throws Exception {
		Collection<ShptObject<?>> ret = super.findDependents(service, marshaled, ctx);
		return ret;
	}
}