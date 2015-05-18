package com.armedia.cmf.engine.documentum;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.armedia.cmf.storage.AttributeTranslator;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.OrganizationStrategy;

public class DocumentumOrganizationStrategy extends OrganizationStrategy {

	public static final String NAME = "documentum";

	public DocumentumOrganizationStrategy() {
		super(DocumentumOrganizationStrategy.NAME);
	}

	@Override
	protected List<String> calculatePath(AttributeTranslator<?> translator, StoredObject<?> object) {
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
		// 06/00a92b/800/54d/0600a92b80054db8
		String uniqueId = objectId.substring(8);
		String[] components = {
			objectId.substring(0, 2), // The object type
			objectId.substring(2, 8), // The docbase ID
			uniqueId.substring(0, 3), // The first 3 characters of the unique object ID
			uniqueId.substring(3, 6), // The 2nd 3 characters of the unique object ID
			objectId
		};
		return new ArrayList<String>(Arrays.asList(components));
	}
}