/**
 *
 */

package com.armedia.cmf.documentum.engine.exporter;

import com.armedia.cmf.documentum.engine.DctmObjectType;
import com.armedia.cmf.storage.ContentStreamStore;
import com.armedia.cmf.storage.StoredObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.content.IDfContent;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class DctmExportContent extends DctmExportAbstract<IDfContent> {

	protected DctmExportContent() {
		super(DctmObjectType.CONTENT);
	}

	@Override
	protected void doStoreContent(IDfSession session, StoredObject<IDfValue> marshaled, IDfContent object,
		ContentStreamStore streamStore) throws Exception {
	}

}