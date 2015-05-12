package com.delta.cmsmf.utils;

import java.util.Arrays;

import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.URIStrategy;
import com.armedia.commons.utilities.FileNameTools;

public class CMSMFDocumentumURIStrategy extends URIStrategy {

	public CMSMFDocumentumURIStrategy() {
		super("cmsmf");
	}

	@Override
	protected String calculateSSP(ObjectStorageTranslator<?> translator, StoredObject<?> object) {
		final String objectId = object.getId();
		if (objectId.length() != 16) { return null; }

		try {
			Long.parseLong(objectId, 16);
		} catch (NumberFormatException e) {
			// Not a documentum ID, so do something else
			return null;
		}

		// 16 character object id in dctm consists of first 2 chars of obj type, next 6
		// chars of docbase id in hex and last 8 chars server generated.
		// For ex: if the id is 0600a92b80054db8 than the SSP would be
		// /80/05/4d/0600a92b80054db8
		String uniqueId = objectId.substring(8);
		String[] components = {
			uniqueId.substring(0, 2), // The first 3 characters of the unique object ID
			uniqueId.substring(2, 4), // The 2nd 3 characters of the unique object ID
			uniqueId.substring(4, 6), // The first 3 characters of the unique object ID
			objectId
		};
		return FileNameTools.reconstitute(Arrays.asList(components), false, false, '/');
	}
}