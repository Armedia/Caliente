package com.armedia.cmf.documentum.engine;

import java.util.Arrays;

import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.URIStrategy;
import com.armedia.commons.utilities.FileNameTools;

public class DocumentumURIStrategy extends URIStrategy {

	public DocumentumURIStrategy() {
		super("documentum");
	}

	@Override
	public String calculateSSP(StoredObjectType objectType, String objectId) {
		String relative = String.format("%s/%s", objectType, objectId);
		if (objectId.length() == 16) {
			try {
				Long.parseLong(objectId, 16);
				// 16 character object id in dctm consists of first 2 chars of obj type, next 6
				// chars of docbase id in hex and last 8 chars server generated. We will use first 6
				// characters of this last 8 characters and generate the unique path.
				// For ex: if the id is 0600a92b80054db8 than the path would be b8/4d/05
				String uniqueId = objectId.substring(8);
				String[] components = {
					objectId.substring(0, 2), // The object type
					objectId.substring(2, 8), // The docbase ID
					uniqueId.substring(0, 3), // The first 3 characters of the unique object ID
					uniqueId.substring(3, 6), // The 2nd 3 characters of the unique object ID
					objectId
				};
				relative = FileNameTools.reconstitute(Arrays.asList(components), false, false, '/');
			} catch (NumberFormatException e) {
				// Not a documentum ID, so do something else
			}
		}
		return relative;
	}
}