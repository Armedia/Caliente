package com.armedia.cmf.engine.sharepoint;

import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.URIStrategy;
import com.armedia.commons.utilities.FileNameTools;

public class SharepointURIStrategy extends URIStrategy {

	public SharepointURIStrategy() {
		super("sharepoint");
	}

	@Override
	protected String calculateSSP(StoredObjectType objectType, String objectId) {
		objectId = FileNameTools.normalizePath(objectId, '/');
		return FileNameTools.removeEdgeSeparators(objectId, '/');
	}
}