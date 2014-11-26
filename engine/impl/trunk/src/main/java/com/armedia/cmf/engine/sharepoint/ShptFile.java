package com.armedia.cmf.engine.sharepoint;

import com.armedia.cmf.storage.StoredObjectType;
import com.independentsoft.share.File;

public class ShptFile extends ShptObject<File> {

	public ShptFile(File wrapped) {
		super(wrapped);
	}

	@Override
	public String getId() {
		return this.wrapped.getUniqueId();
	}

	@Override
	public StoredObjectType getStoredType() {
		return StoredObjectType.DOCUMENT;
	}
}