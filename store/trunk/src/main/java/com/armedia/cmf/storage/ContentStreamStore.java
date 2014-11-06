package com.armedia.cmf.storage;

import java.io.File;

public final class ContentStreamStore {

	private final File baseLocation;

	public ContentStreamStore(File baseLocation) {
		this.baseLocation = baseLocation;
	}

	public final File getStreamLocation(StoredObject<?> object) {
		String relative = object.getRelativeStreamLocation();
		if (relative == null) { return null; }
		return new File(this.baseLocation, relative);
	}

	protected String getRelativeStreamLocation(StoredObject<?> object, Long objectNumber) {
		return null;
	}
}