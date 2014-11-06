/**
 *
 */

package com.armedia.cmf.documentum.engine.exporter;

import com.armedia.cmf.documentum.engine.DctmObjectType;
import com.armedia.cmf.storage.ContentStreamStore;
import com.documentum.fc.client.IDfDocument;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;

/**
 * @author diego
 *
 */
public class DctmContentExporter extends DctmExporter<IDfDocument> {

	protected DctmContentExporter() {
		super(DctmObjectType.CONTENT);
	}

	@Override
	public void storeContent(IDfSession session, IDfPersistentObject object, ContentStreamStore streamStore)
		throws Exception {
		// TODO: Store the content
	}

}