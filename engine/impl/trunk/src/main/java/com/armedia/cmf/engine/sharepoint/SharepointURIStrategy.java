package com.armedia.cmf.engine.sharepoint;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.URIStrategy;
import com.armedia.commons.utilities.FileNameTools;

public class SharepointURIStrategy extends URIStrategy {

	public SharepointURIStrategy() {
		super("sharepoint");
	}

	protected String encodeSafePathComponent(String pathComponent) {
		try {
			return URLEncoder.encode(pathComponent, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("UTF-8 encoding not supported in this JVM", e);
		}
	}

	@Override
	protected String calculateSSP(ObjectStorageTranslator<?> translator, StoredObject<?> object) {
		// Put it in the same path as it was in Sharepoint, but ensure each path component is
		// of a "universally-valid" format.
		String searchKey = object.getSearchKey();
		final boolean leading = searchKey.startsWith("/");
		List<String> pathItems = new ArrayList<String>();
		for (String s : FileNameTools.tokenize(searchKey, '/')) {
			pathItems.add(encodeSafePathComponent(s));
		}
		return FileNameTools.reconstitute(pathItems, leading, false, '/');
	}
}