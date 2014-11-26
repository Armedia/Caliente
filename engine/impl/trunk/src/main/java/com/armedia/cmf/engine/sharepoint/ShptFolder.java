package com.armedia.cmf.engine.sharepoint;

import com.armedia.cmf.storage.StoredObjectType;
import com.independentsoft.share.Folder;

public class ShptFolder extends ShptObject<Folder> {

	public ShptFolder(Folder wrapped) {
		super(wrapped);
	}

	@Override
	public String getId() {
		return this.wrapped.getUniqueId();
	}

	@Override
	public StoredObjectType getStoredType() {
		return StoredObjectType.FOLDER;
	}
}