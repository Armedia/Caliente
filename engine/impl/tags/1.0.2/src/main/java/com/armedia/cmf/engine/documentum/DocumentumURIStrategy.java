package com.armedia.cmf.engine.documentum;

import java.util.Arrays;

import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.URIStrategy;
import com.armedia.commons.utilities.FileNameTools;

public class DocumentumURIStrategy extends URIStrategy {

	public DocumentumURIStrategy() {
		super("documentum");
	}

	@Override
	protected String calculateSSP(StoredObjectType objectType, String objectId) {
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
		// 06/00a92b/800/54d/0600a92b80054db8
		String uniqueId = objectId.substring(8);
		String[] components = {
			objectId.substring(0, 2), // The object type
			objectId.substring(2, 8), // The docbase ID
			uniqueId.substring(0, 3), // The first 3 characters of the unique object ID
			uniqueId.substring(3, 6), // The 2nd 3 characters of the unique object ID
			objectId
		};
		return FileNameTools.reconstitute(Arrays.asList(components), false, false, '/');
	}
}