package com.armedia.cmf.engine.sharepoint;

import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.URIStrategy;

public class SharepointURIStrategy extends URIStrategy {

	public SharepointURIStrategy() {
		super("sharepoint");
	}

	@Override
	protected String calculateSSP(StoredObject<?> object) {
		// Put it in the same path as it was in Sharepoint...
		return object.getSearchKey();
	}
}